// Copyright 2021-2022 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.jsmulti;

import io.nats.client.*;
import io.nats.client.api.*;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;
import io.nats.jsmulti.shared.ActionRunner;
import io.nats.jsmulti.shared.Application;
import io.nats.jsmulti.shared.Stats;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import static io.nats.jsmulti.shared.Utils.*;

/**
 * The JsMulti Main class
 * Various ways to run the code
 * 1. Through an ide...
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.JsMulti -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean jsMulti --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `jsMulti` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.JsMulti [args]
 *    ! You must have run gradle clean jar and know where the jnats library is
 * 5. Command Line: java -cp <path-to-uber-jar> io.nats.jsmulti.JsMulti [args]
 *    ! You must have run gradle clean uberJar
 */
public class JsMulti {

    public static void main(String[] args) throws Exception {
        run(new Context(args), false, true);
    }

    public static List<Stats> run(String[] args) throws Exception {
        return run(new Context(args), false, true);
    }

    public static List<Stats> run(String[] args, boolean printArgs, boolean reportWhenDone) throws Exception {
        return run(new Context(args), printArgs, reportWhenDone);
    }

    public static List<Stats> run(Arguments args) throws Exception {
        return run(new Context(args), false, true);
    }

    public static List<Stats> run(Arguments args, boolean printArgs, boolean reportWhenDone) throws Exception {
        return run(new Context(args), printArgs, reportWhenDone);
    }

    public static List<Stats> run(Context ctx) throws Exception {
        return run(ctx, false, true);
    }

    public static List<Stats> run(Context ctx, boolean printArgs, boolean reportWhenDone) throws Exception {
        if (printArgs) {
            System.out.println(ctx);
        }

        ActionRunner runner = getRunner(ctx);
        List<Stats> statsList = ctx.connShared
            ? runShared(ctx, runner)
            : runIndividual(ctx, runner);

        try (Connection nc = connect(ctx)) {
            JetStreamManagement jsm = nc.jetStreamManagement(ctx.getJetStreamOptions());
            List<String> streams = jsm.getStreamNames();
            for (String stream : streams) {
                try {
                    List<String> cons = jsm.getConsumerNames(stream);
                    for (String con : cons) {
                        if (ctx.subDurables.contains(con)) {
                            ctx.subDurables.remove(con);
                            jsm.deleteConsumer(stream, con);
                        }
                    }
                }
                catch (Exception ignore) {}
            }
        }
        catch (Exception ignore) {}

        if (reportWhenDone && ctx.action != Action.REPLY) {
            Stats.report(statsList);
        }
        return statsList;
    }

    private static ActionRunner getRunner(final Context ctx) {
        try {
            switch (ctx.action) {
                case CUSTOM:    return ctx.customActionRunner;

                case PUB_SYNC:  return JsMulti::pubSync;
                case PUB_ASYNC: return JsMulti::pubAsync;
                case PUB_CORE:  return JsMulti::pubCore;
                case PUB:       return JsMulti::pub;

                case RTT:       return JsMulti::rtt;

                case REQUEST:   return JsMulti::request;
                case REPLY:     return JsMulti::reply;

                case SUB_CORE:  return JsMulti::subCore;
                case SUB_PUSH:  return JsMulti::subPush;

                case SUB_PULL:
                case SUB_PULL_READ:
                    return JsMulti::subPull;

                case SUB_QUEUE:
                    if (ctx.threads > 1) {
                        return JsMulti::subPush;
                    }
                    break;

                case SUB_PULL_QUEUE:
                case SUB_PULL_READ_QUEUE:
                    if (ctx.threads > 1) {
                        return JsMulti::subPull;
                    }
                    break;
            }
            throw new Exception("Invalid Action");
        }
        catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // RTT
    // ----------------------------------------------------------------------------------------------------
    private static void rtt(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        int pubTarget = ctx.getPubCount(id);
        int published = 0;
        int unReported = 0;
        report(published, "Begin RTT", ctx.app);
        while (published < pubTarget) {
            jitter(ctx);
            stats.manualElapsed(nc.RTT().toNanos(), 1);
            unReported = reportMaybe(ctx, ++published, ++unReported, "RTTs so far");
        }
        report(published, "RTTs completed", ctx.app);
    }

    // ----------------------------------------------------------------------------------------------------
    // Publish
    // ----------------------------------------------------------------------------------------------------
    interface Publisher<T> {
        T publish(String subject, byte[] payload) throws Exception;
    }

    interface ResultHandler<T> {
        void handle(T t);
    }

    private static NatsMessage buildLatencyMessage(String subject, byte[] p) {
        //noinspection ConstantConditions
        return new NatsMessage(subject, null, new Headers().put(HDR_PUB_TIME, "" + System.currentTimeMillis()), p);
    }

    private static void pub(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        if (ctx.latencyFlag) {
            _pub(ctx, stats, id, (s, p) -> {
                nc.publish(buildLatencyMessage(s, p));
                return true;
            }, b -> {});
        }
        else {
            _pub(ctx, stats, id, (s, p) -> {
                nc.publish(s, p);
                return true;
            }, b -> {});
        }

        // if you are using pub with a consumer on the other side,
        // sometimes if you disconnect before all publishes have completed
        // the publishes don't actually take.
        Thread.sleep(ctx.postPubWaitMillis);
    }

    private static void request(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        if (ctx.action.isPubAsync()) {
            _pub(ctx, stats, id, nc::request, cfm -> {});
        }
        else {
            _pub(ctx, stats, id, (s, p) -> nc.request(s, p, ctx.requestWaitDuration), m -> {});
        }
    }

    private static void pubSync(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        final JetStream js = nc.jetStream(ctx.getJetStreamOptions());
        if (ctx.latencyFlag) {
            _pub(ctx, stats, id, (s, p) -> js.publish(buildLatencyMessage(s, p)), na -> {});
        }
        else {
            _pub(ctx, stats, id, js::publish, na -> {});
        }
    }

    private static void pubCore(Context ctx, final Connection nc, Stats stats, int id) throws Exception {
        // if you are using pub core to test latency, that's okay but...
        // sometimes if you disconnect before all publishes have completed
        // the publishes don't actually take. I think this is a matter
        // of how pub acks work. So we are going to try to make sure the message show up.
        JetStreamManagement jsm = null;
        String streamName = null;
        long startingCount = -1;
        if (ctx.latencyFlag) {
            jsm = nc.jetStreamManagement(ctx.getJetStreamOptions());
            List<String> streamNames = jsm.getStreamNames(ctx.subject);
            if (streamNames.size() != 1) {
                throw new RuntimeException("JetStream subject does not exist for latency run [" + ctx.subject + "]");
            }
            streamName = streamNames.get(0);
            StreamInfo si = jsm.getStreamInfo(streamName, StreamInfoOptions.filterSubjects(ctx.subject));
            List<Subject> subjects = si.getStreamState().getSubjects();
            startingCount = subjects == null ? 0 : subjects.get(0).getCount();
        }

        Publisher<PublishAck> publisher = ctx.latencyFlag
            ? (s, p) -> { nc.publish(buildLatencyMessage(s, p)); return null; }
            : (s, p) -> { nc.publish(s, p); return null; };

        _pub(ctx, stats, id, publisher, pa -> {});

        if (startingCount != -1) {
            long currentCount = 0;
            while (currentCount < ctx.messageCount) {
                StreamInfo si = jsm.getStreamInfo(streamName, StreamInfoOptions.filterSubjects(ctx.subject));
                currentCount = si.getStreamState().getSubjects().get(0).getCount();
                if (currentCount < ctx.messageCount) {
                    ctx.app.report("Waiting for the server to record all publishes. " + currentCount + " of " + ctx.messageCount);
                    Thread.sleep(100);
                }
            }
        }
    }

    private static <T> void _pub(Context ctx, Stats stats, int id, Publisher<T> p, ResultHandler<T> rh) throws Exception {
        int retriesAvailable = ctx.maxPubRetries;
        int pubTarget = ctx.getPubCount(id);
        int published = 0;
        int unReported = 0;
        report(published, "Begin Publishing", ctx.app);
        while (published < pubTarget) {
            jitter(ctx);
            byte[] payload = ctx.getPayload();
            stats.start();
            try {
                rh.handle(p.publish(ctx.subject, payload));
                stats.stopAndCount(ctx.payloadSize);
                unReported = reportMaybe(ctx, ++published, ++unReported, "Published");
            }
            catch (IOException ioe) {
                if (!isRegularTimeout(ioe) || --retriesAvailable == 0) { throw ioe; }
            }
        }
        report(published, "Completed Publishing", ctx.app);
    }

    private static void pubAsync(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        JetStream js = nc.jetStream(ctx.getJetStreamOptions());
        Publisher<CompletableFuture<PublishAck>> publisher;
        if (ctx.latencyFlag) {
            publisher = (s, p) -> js.publishAsync(buildLatencyMessage(s, p));
        }
        else {
            publisher = js::publishAsync;
        }

        List<CompletableFuture<PublishAck>> futures = new ArrayList<>();
        int roundCount = 0;
        int pubTarget = ctx.getPubCount(id);
        int published = 0;
        int unReported = 0;
        report(published, "Begin Publishing", ctx.app);
        while (published < pubTarget) {
            if (++roundCount >= ctx.roundSize) {
                processFutures(futures, stats);
                roundCount = 0;
            }
            jitter(ctx);
            byte[] payload = ctx.getPayload();
            stats.start();
            futures.add(publisher.publish(ctx.subject, payload));
            stats.stopAndCount(ctx.payloadSize);
            unReported = reportMaybe(ctx, ++published, ++unReported, "Published");
        }
        report(published, "Completed Publishing", ctx.app);
    }

    private static void processFutures(List<CompletableFuture<PublishAck>> futures, Stats stats) {
        stats.start();
        while (!futures.isEmpty()) {
            try {
                futures.remove(0).get();
            }
            catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        stats.stop();
    }

    // ----------------------------------------------------------------------------------------------------
    // Push
    // ----------------------------------------------------------------------------------------------------
    private static final Object QUEUE_LOCK = new Object();

    private static void reply(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        Subscription sub;
        if (ctx.action.isQueue()) {
            // if we don't do this, multiple threads will try to make the same consumer because
            // when they start, the consumer does not exist. So force them do it one at a time.
            synchronized (QUEUE_LOCK) {
                sub = nc.subscribe(ctx.subject, ctx.queueName);
            }
        }
        else {
            sub = nc.subscribe(ctx.subject);
        }

        _coreReadLikePush(ctx, stats, ctx.getSubName(id), sub::nextMessage, m -> nc.publish(m.getReplyTo(), m.getData()));
    }

    private static void subCore(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        Subscription sub;
        if (ctx.action.isQueue()) {
            // if we don't do this, multiple threads will try to make the same consumer because
            // when they start, the consumer does not exist. So force them do it one at a time.
            synchronized (QUEUE_LOCK) {
                sub = nc.subscribe(ctx.subject, ctx.queueName);
            }
        }
        else {
            sub = nc.subscribe(ctx.subject);
        }

        _coreReadLikePush(ctx, stats, ctx.getSubName(id), sub::nextMessage, m -> {});
    }

    private static void _coreReadLikePush(Context ctx, Stats stats, String subName, SimpleReader reader, ResultHandler<Message> rh) throws InterruptedException {
        int rcvd = 0;
        int unReported = 0;
        long noMessageTotalElapsed = 0;
        AtomicLong counter = ctx.getSubscribeCounter(subName);
        report(rcvd, "Begin Reading", ctx.app);
        while (counter.get() < ctx.messageCount) {
            stats.start();
            Message m = reader.nextMessage(ctx.readTimeoutDuration);
            long hold = stats.elapsed();
            long received = System.currentTimeMillis();
            if (m == null) {
                noMessageTotalElapsed += hold;
                if (noMessageTotalElapsed > ctx.readMaxWaitDuration.toMillis()) {
                    report(rcvd, "Stopped At Max Wait, Finished Reading Messages", ctx.app);
                    return;
                }
                acceptHoldOnceStarted(stats, rcvd, hold, ctx.app);
            }
            else {
                noMessageTotalElapsed = 0;
                rh.handle(m);
                stats.manualElapsed(hold);
                stats.count(m, received);
                counter.incrementAndGet();
                unReported = reportMaybe(ctx, ++rcvd, ++unReported, "Messages Read");
            }
        }
        report(rcvd, "Finished Reading Messages", ctx.app);
    }

    private static void subPush(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        JetStream js = nc.jetStream(ctx.getJetStreamOptions());
        JetStreamSubscription sub;
        String durable = ctx.getSubDurable(id);
        if (ctx.action.isQueue()) {
            // if we don't do this, multiple threads will try to make the same consumer because
            // when they start, the consumer does not exist. So force them do it one at a time.
            synchronized (QUEUE_LOCK) {
                sub = js.subscribe(ctx.subject, ctx.queueName,
                    ConsumerConfiguration.builder()
                        .ackPolicy(ctx.ackPolicy)
                        .ackWait(Duration.ofSeconds(ctx.ackWaitSeconds))
                        .durable(durable)
                        .deliverGroup(ctx.queueName)
                        .buildPushSubscribeOptions());
            }
        }
        else {
            sub = js.subscribe(ctx.subject,
                ConsumerConfiguration.builder()
                    .ackPolicy(ctx.ackPolicy)
                    .ackWait(Duration.ofSeconds(ctx.ackWaitSeconds))
                    .durable(durable)
                    .buildPushSubscribeOptions());
        }

        _jsReadLikePush(ctx, stats, durable, sub::nextMessage);
    }

    // ----------------------------------------------------------------------------------------------------
    // Simple Reader - Used by push and pull reader
    // ----------------------------------------------------------------------------------------------------
    interface SimpleReader {
        Message nextMessage(Duration timeout) throws InterruptedException, IllegalStateException;
    }

    private static void _jsReadLikePush(Context ctx, Stats stats, String durable, SimpleReader reader) throws InterruptedException {
        int rcvd = 0;
        Message lastUnAcked = null;
        int unAckedCount = 0;
        int unReported = 0;
        long noMessageTotalElapsed = 0;
        AtomicLong counter = ctx.getSubscribeCounter(durable);
        report(rcvd, "Begin Reading", ctx.app);
        while (counter.get() < ctx.messageCount) {
            stats.start();
            Message m = reader.nextMessage(ctx.readTimeoutDuration);
            long hold = stats.elapsed();
            long received = System.currentTimeMillis();
            if (m == null) {
                noMessageTotalElapsed += hold;
                if (noMessageTotalElapsed > ctx.readMaxWaitDuration.toMillis()) {
                    report(rcvd, "Stopped At Max Wait, Finished Reading Messages", ctx.app);
                    return;
                }
                acceptHoldOnceStarted(stats, rcvd, hold, ctx.app);
            }
            else {
                noMessageTotalElapsed = 0;
                stats.manualElapsed(hold);
                stats.count(m, received);
                counter.incrementAndGet();
                if ( (lastUnAcked = ackMaybe(ctx, stats, m, ++unAckedCount)) == null ) {
                    unAckedCount = 0;
                }
                unReported = reportMaybe(ctx, ++rcvd, ++unReported, "Messages Read");
            }
        }
        if (lastUnAcked != null) {
            _ack(stats, lastUnAcked);
        }
        report(rcvd, "Finished Reading Messages", ctx.app);
    }

    // ----------------------------------------------------------------------------------------------------
    // Pull
    // ----------------------------------------------------------------------------------------------------
    private static void subPull(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        JetStream js = nc.jetStream(ctx.getJetStreamOptions());

        // Really only need to lock when queueing b/c it's the same durable...
        // ... to ensure protection from multiple threads trying to make the same consumer
        String durable = ctx.getSubDurable(id);
        JetStreamSubscription sub;
        synchronized (QUEUE_LOCK) {
            sub = js.subscribe(ctx.subject,
                ConsumerConfiguration.builder()
                    .ackPolicy(ctx.ackPolicy)
                    .ackWait(Duration.ofSeconds(ctx.ackWaitSeconds))
                    .durable(durable)
                    .buildPullSubscribeOptions());
        }

        if (ctx.action == Action.SUB_PULL || ctx.action == Action.SUB_PULL_QUEUE) {
            _subPullFetch(ctx, stats, sub, durable);
        }
        else {
            _subPullRead(ctx, stats, sub, durable);
        }
    }

    private static void _subPullFetch(Context ctx, Stats stats, JetStreamSubscription sub, String durable) {
        int rcvd = 0;
        Message lastUnAcked = null;
        int unAckedCount = 0;
        int unReported = 0;
        AtomicLong counter = ctx.getSubscribeCounter(durable);
        report(rcvd, "Begin Reading", ctx.app);
        while (counter.get() < ctx.messageCount) {
            stats.start();
            List<Message> list = sub.fetch(ctx.batchSize, ctx.readTimeoutDuration);
            long hold = stats.elapsed();
            long received = System.currentTimeMillis();
            int lc = list.size();
            if (lc > 0) {
                for (Message m : list) {
                    stats.count(m, received);
                    counter.incrementAndGet();
                    if ((lastUnAcked = ackMaybe(ctx, stats, m, ++unAckedCount)) == null) {
                        unAckedCount = 0;
                    }
                }
                rcvd += lc;
                unReported = reportMaybe(ctx, rcvd, unReported + lc, "Messages Read");
            }
            acceptHoldOnceStarted(stats, rcvd, hold, ctx.app);
        }
        if (lastUnAcked != null) {
            _ack(stats, lastUnAcked);
        }
        report(rcvd, "Finished Reading Messages", ctx.app);
    }

    private static void _subPullRead(Context ctx, Stats stats, JetStreamSubscription sub, String durable) throws InterruptedException {
        JetStreamReader reader = sub.reader(ctx.batchSize, ctx.batchSize / 4); // repullAt 25% of batch size
        _jsReadLikePush(ctx, stats, durable, reader::nextMessage);
        reader.stop();
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------
    private static void acceptHoldOnceStarted(Stats stats, int rcvd, long hold, Application jsmApp) {
        if (rcvd == 0) {
            jsmApp.report("Waiting for first message.");
        }
        else {
            // not the first message so we count waiting time
            stats.manualElapsed(hold);
        }
    }

    private static boolean isRegularTimeout(IOException ioe) {
        return ioe.getMessage().equals("Timeout or no response waiting for NATS JetStream server");
    }

    // This method returns null if message is acked or policy is None
    private static Message ackMaybe(Context ctx, Stats stats, Message m, int unAckedCount) {
        if (ctx.ackPolicy == AckPolicy.Explicit) {
            _ack(stats, m);
            return null;
        }
        if (ctx.ackPolicy == AckPolicy.All) {
            if (unAckedCount >= ctx.ackAllFrequency) {
                _ack(stats, m);
                return null;
            }
            return m;
        }
        // AckPolicy.None
        return null;
    }

    private static void _ack(Stats stats, Message m) {
        stats.start();
        m.ack();
        stats.stop();
    }

    private static Connection connect(Context ctx) throws Exception {
        Options options = ctx.getOptions();
        Connection nc = Nats.connect(options);
        for (long x = 0; x < 100; x++) { // waits up to 10 seconds (100 * 100 = 10000) millis to be connected
            sleep(100);
            if (nc.getStatus() == Connection.Status.CONNECTED) {
                return nc;
            }
        }
        return nc;
    }

    private static List<Stats> runShared(Context ctx, ActionRunner runner) throws Exception {
        try (Connection nc = connect(ctx)) {
            List<Stats> statsList = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (int x = 0; x < ctx.threads; x++) {
                final int id = x + 1;
                final Stats stats = new Stats(ctx);
                statsList.add(stats);
                Thread t = new Thread(() -> {
                    try {
                        runner.run(ctx, nc, stats, id);
                    } catch (Exception e) {
                        ctx.app.reportEx(e);
                    }
                }, ctx.getLabel(id));
                threads.add(t);
            }
            return endRun(statsList, threads);
        }
    }

    private static List<Stats> runIndividual(Context ctx, ActionRunner runner) throws Exception {
        List<Stats> statsList = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int x = 0; x < ctx.threads; x++) {
            final int id = x + 1;
            final Stats stats = new Stats(ctx);
            statsList.add(stats);
            Thread t = new Thread(() -> {
                try (Connection nc = connect(ctx)) {
                    runner.run(ctx, nc, stats, id);
                } catch (Exception e) {
                    ctx.app.reportEx(e);
                }
            }, ctx.getLabel(id));
            threads.add(t);
        }
        return endRun(statsList, threads);
    }

    private static List<Stats> endRun(List<Stats> statsList, List<Thread> threads) throws InterruptedException {
        for (Thread t : threads) { t.start(); }
        for (Thread t : threads) { t.join(); }
        for (Stats s : statsList) {
            s.shutdown();
        }
        boolean notTerminated = true;
        while (notTerminated) {
            Thread.sleep(100);
            notTerminated = false;
            for (Stats s : statsList) {
                if (!s.isTerminated()) {
                    notTerminated = true;
                }
            }
        }
        return statsList;
    }
}

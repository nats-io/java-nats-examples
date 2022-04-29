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
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;
import io.nats.jsmulti.shared.Stats;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static io.nats.jsmulti.shared.Utils.*;

/**
 * The main class
 *
 * Various ways to run the code
 * 1. Through an ide...
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.JsMulti -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean consumer --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `jsMulti` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.JsMulti [args]
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

        Runner runner = getRunner(ctx);
        List<Stats> statsList = ctx.connShared ? runShared(ctx, runner) : runIndividual(ctx, runner);

        if (reportWhenDone) {
            Stats.report(statsList);
        }
        return statsList;
    }

    private static Runner getRunner(Context ctx) {
        try {
            switch (ctx.action) {
                case PUB_SYNC:
                    return (nc, stats, id) -> pubSync(ctx, nc, stats, id);
                case PUB_ASYNC:
                    return (nc, stats, id) -> pubAsync(ctx, nc, stats, id);
                case PUB_CORE:
                    return (nc, stats, id) -> pubCore(ctx, nc, stats, id);
                case SUB_PUSH:
                    return (nc, stats, id) -> subPush(ctx, nc, stats, id);
                case SUB_PULL:
                    return (nc, stats, id) -> subPull(ctx, nc, stats, id);
                case SUB_QUEUE:
                    if (ctx.threads > 1) {
                        return (nc, stats, id) -> subPush(ctx, nc, stats, id);
                    }
                    break;
                case SUB_PULL_QUEUE:
                    if (ctx.threads > 1) {
                        return (nc, stats, id) -> subPull(ctx, nc, stats, id);
                    }
                    break;
            }
            throw new Exception("Invalid Action");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Publish
    // ----------------------------------------------------------------------------------------------------
    interface Publisher<T> {
        T publish(String subject, byte[] payload) throws Exception;
    }

    private static NatsMessage buildLatencyMessage(String subject, byte[] p) {
        //noinspection ConstantConditions
        return new NatsMessage(subject, null, new Headers().put(HDR_PUB_TIME, "" + System.currentTimeMillis()), p);
    }

    private static void pubSync(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        final JetStream js = nc.jetStream(ctx.getJetStreamOptions());
        if (ctx.latencyFlag) {
            _pub(ctx, stats, id, (s, p) -> js.publish(buildLatencyMessage(s, p)));
        }
        else {
            _pub(ctx, stats, id, js::publish);
        }
    }

    private static void pubCore(Context ctx, final Connection nc, Stats stats, int id) throws Exception {
        _pub(ctx, stats, id, ctx.latencyFlag
            ? (s, p) -> { nc.publish(buildLatencyMessage(s, p)); return null; }
            : (s, p) -> { nc.publish(s, p); return null; } );
    }

    private static void _pub(Context ctx, Stats stats, int id, Publisher<PublishAck> p) throws Exception {
        int retriesAvailable = ctx.maxPubRetries;
        int pubTarget = ctx.getPubCount(id);
        int published = 0;
        int unReported = 0;
        report(published, "Begin Publishing");
        while (published < pubTarget) {
            jitter(ctx);
            byte[] payload = ctx.getPayload();
            stats.start();
            try {
                p.publish(ctx.subject, payload);
                stats.stopAndCount(ctx.payloadSize);
                unReported = reportMaybe(ctx, ++published, ++unReported, "Published");
            }
            catch (IOException ioe) {
                if (!isRegularTimeout(ioe) || --retriesAvailable == 0) { throw ioe; }
            }
        }
        report(published, "Completed Publishing");
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
        report(published, "Begin Publishing");
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
        report(published, "Completed Publishing");
    }

    private static void processFutures(List<CompletableFuture<PublishAck>> futures, Stats stats) {
        stats.start();
        while (futures.size() > 0) {
            CompletableFuture<PublishAck> f = futures.remove(0);
            if (!f.isDone()) {
                futures.add(f);
            }
        }
        stats.stop();
    }

    // ----------------------------------------------------------------------------------------------------
    // Push
    // ----------------------------------------------------------------------------------------------------
    private static final Object QUEUE_LOCK = new Object();

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

        int rcvd = 0;
        Message lastUnAcked = null;
        int unAckedCount = 0;
        int unReported = 0;
        AtomicLong counter = ctx.getSubscribeCounter(durable);
        report(rcvd, "Begin Reading");
        while (counter.get() < ctx.messageCount) {
            stats.start();
            Message m = sub.nextMessage(Duration.ofSeconds(1));
            long hold = stats.elapsed();
            long received = System.currentTimeMillis();
            if (m == null) {
                acceptHoldOnceStarted(stats, rcvd, hold);
            }
            else {
                stats.acceptHold(hold);
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
        report(rcvd, "Finished Reading Messages");
    }

    // ----------------------------------------------------------------------------------------------------
    // Pull
    // ----------------------------------------------------------------------------------------------------
    private static void subPull(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        JetStream js = nc.jetStream(ctx.getJetStreamOptions());

        // Really only need to lock when queueing b/c it's the same durable...
        // To ensure protection from multiple threads trying  make the same consumer because
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

        _subPullFetch(ctx, stats, sub, durable);
    }

    private static void _subPullFetch(Context ctx, Stats stats, JetStreamSubscription sub, String durable) {
        int rcvd = 0;
        Message lastUnAcked = null;
        int unAckedCount = 0;
        int unReported = 0;
        AtomicLong counter = ctx.getSubscribeCounter(durable);
        report(rcvd, "Begin Reading");
        while (counter.get() < ctx.messageCount) {
            stats.start();
            List<Message> list = sub.fetch(ctx.batchSize, Duration.ofMillis(500));
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
            acceptHoldOnceStarted(stats, rcvd, hold);
        }
        if (lastUnAcked != null) {
            _ack(stats, lastUnAcked);
        }
        report(rcvd, "Finished Reading Messages");
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------
    private static void acceptHoldOnceStarted(Stats stats, int rcvd, long hold) {
        if (rcvd == 0) {
            log("Waiting for first message.");
        }
        else {
            // not the first message so we count waiting time
            stats.acceptHold(hold);
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

    private static void report(int total, String message) {
        log(message + " " + Stats.format(total));
    }

    private static int reportMaybe(Context ctx, int total, int unReported, String message) {
        if (unReported >= ctx.reportFrequency) {
            report(total, message);
            return 0; // there are 0 unreported now
        }
        return unReported;
    }

    private static void jitter(Context ctx) {
        if (ctx.jitter > 0) {
            sleep(ThreadLocalRandom.current().nextLong(ctx.jitter));
        }
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

    // ----------------------------------------------------------------------------------------------------
    // Runners
    // ----------------------------------------------------------------------------------------------------
    interface Runner {
        void run(Connection nc, Stats stats, int id) throws Exception;
    }

    private static List<Stats> runShared(Context ctx, Runner runner) throws Exception {
        List<Stats> statsList = new ArrayList<>();
        try (Connection nc = connect(ctx)) {
            List<Thread> threads = new ArrayList<>();
            for (int x = 0; x < ctx.threads; x++) {
                final int id = x + 1;
                final Stats stats = new Stats(ctx.action);
                statsList.add(stats);
                Thread t = new Thread(() -> {
                    try {
                        runner.run(nc, stats, id);
                    } catch (Exception e) {
                        logEx(e);
                    }
                }, ctx.getLabel(id));
                threads.add(t);
            }
            for (Thread t : threads) { t.start(); }
            for (Thread t : threads) { t.join(); }
        }
        return statsList;
    }

    private static List<Stats> runIndividual(Context ctx, Runner runner) throws Exception {
        List<Stats> statsList = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int x = 0; x < ctx.threads; x++) {
            final int id = x + 1;
            final Stats stats = new Stats(ctx.action);
            statsList.add(stats);
            Thread t = new Thread(() -> {
                try (Connection nc = connect(ctx)) {
                    runner.run(nc, stats, id);
                } catch (Exception e) {
                    logEx(e);
                }
            }, ctx.getLabel(id));
            threads.add(t);
        }
        for (Thread t : threads) { t.start(); }
        for (Thread t : threads) { t.join(); }

        return statsList;
    }
}

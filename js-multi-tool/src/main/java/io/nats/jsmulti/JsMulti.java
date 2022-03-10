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
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import io.nats.jsmulti.internal.Context;
import io.nats.jsmulti.internal.Publisher;
import io.nats.jsmulti.internal.Runner;
import io.nats.jsmulti.shared.OptionsFactory;
import io.nats.jsmulti.shared.Stats;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static io.nats.jsmulti.shared.Utils.*;

public class JsMulti {

    public static void main(String[] args) throws Exception {
        run(new Context(args), false, true);
    }

    public static List<Stats> run(String[] args) throws Exception {
        return run(new Context(args), false, true);
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
                    return (nc, stats, id) -> pubSync(ctx, nc, stats, getLabel(ctx, id));
                case PUB_ASYNC:
                    return (nc, stats, id) -> pubAsync(ctx, nc, stats, getLabel(ctx, id));
                case PUB_CORE:
                    return (nc, stats, id) -> pubCore(ctx, nc, stats, getLabel(ctx, id));
                case SUB_PUSH:
                    return (nc, stats, id) -> subPush(ctx, nc, stats, false, getLabel(ctx, id));
                case SUB_PULL:
                    return (nc, stats, id) -> subPull(ctx, nc, stats, id, getLabel(ctx, id));
                case SUB_QUEUE:
                    if (ctx.threads > 1) {
                        return (nc, stats, id) -> subPush(ctx, nc, stats, true, getLabel(ctx, id));
                    }
                    break;
                case SUB_PULL_QUEUE:
                    if (ctx.threads > 1) {
                        return (nc, stats, id) -> subPull(ctx, nc, stats, id, getLabel(ctx, id));
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

    private static String getLabel(Context ctx, int id) {
        return ctx.action + (ctx.connShared ? "Shared " : "Individual ") + id;
    }

    // ----------------------------------------------------------------------------------------------------
    // Publish
    // ----------------------------------------------------------------------------------------------------
    private static NatsMessage buildLatencyMessage(Context ctx, byte[] p) {
        return NatsMessage.builder()
            .subject(ctx.subject)
            .data(p)
            .headers(new Headers().put(HDR_PUB_TIME, "" + System.currentTimeMillis()))
            .build();
    }

    private static void pubSync(Context ctx, Connection nc, Stats stats, String label) throws Exception {
        final JetStream js = nc.jetStream();
        if (ctx.latencyFlag) {
            _pub(ctx, stats, label, (p) -> js.publish(buildLatencyMessage(ctx, p)));
        }
        else {
            _pub(ctx, stats, label, (p) -> js.publish(ctx.subject, p));
        }
    }

    private static void pubCore(Context ctx, final Connection nc, Stats stats, String label) throws Exception {
        _pub(ctx, stats, label, ctx.latencyFlag
            ? (p) -> { nc.publish(buildLatencyMessage(ctx, p)); return null; }
            : (p) -> { nc.publish(ctx.subject, p); return null; } );
    }

    private static void _pub(Context ctx, Stats stats, String label, Publisher<PublishAck> p) throws Exception {
        int retriesAvailable = ctx.maxPubRetries;
        int x = 1;
        while (x <= ctx.perThread()) {
            jitter(ctx);
            byte[] payload = ctx.getPayload();
            stats.start();
            try {
                p.publish(payload);
                stats.stopAndCount(ctx.payloadSize);
                reportMaybe(ctx, x++, label, "Published");
            }
            catch (IOException ioe) {
                if (!isRegularTimeout(ioe) || --retriesAvailable == 0) { throw ioe; }
            }
        }
        report(x, label, "Completed Publishing");
    }

    private static void pubAsync(final Context ctx, Connection nc, Stats stats, String label) throws Exception {
        JetStream js = nc.jetStream();
        Publisher<CompletableFuture<PublishAck>> publisher;
        if (ctx.latencyFlag) {
            publisher = (p) -> js.publishAsync(ctx.subject, p);
        }
        else {
            publisher = (p) -> js.publishAsync(buildLatencyMessage(ctx, p));
        }

        List<CompletableFuture<PublishAck>> futures = new ArrayList<>();
        int r = 0;
        int x = 1;
        for (; x <= ctx.perThread(); x++) {
            if (++r >= ctx.roundSize) {
                processFutures(futures, stats);
                r = 0;
            }
            jitter(ctx);
            byte[] payload = ctx.getPayload();
            stats.start();
            futures.add(publisher.publish(payload));
            stats.stopAndCount(ctx.payloadSize);
            reportMaybe(ctx, x, label, "Published");
        }
        report(x, label, "Completed Publishing");
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
    private static void subPush(Context ctx, Connection nc, Stats stats, boolean q, String label) throws Exception {
        JetStream js = nc.jetStream();
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
            .ackPolicy(ctx.ackPolicy)
            .build();
        PushSubscribeOptions pso = PushSubscribeOptions.builder()
            .configuration(cc)
            .durable(q ? ctx.queueDurable : null)
            .build();
        JetStreamSubscription sub;
        if (q) {
            // if we don't do this, multiple threads will try to make the same consumer because
            // when they start, the consumer does not exist. So force them do it one at ctx time.
            synchronized (ctx.queueName) {
                sub = js.subscribe(ctx.subject, ctx.queueName, pso);
            }
        }
        else {
            sub = js.subscribe(ctx.subject, pso);
        }

        int x = 0;
        List<Message> ackList = new ArrayList<>();
        while (x < ctx.perThread()) {
            stats.start();
            Message m = sub.nextMessage(Duration.ofSeconds(1));
            if (m == null) {
                continue;
            }
            stats.stopAndCount(m);
            ackMaybe(ctx, stats, ackList, m);
            reportMaybe(ctx, ++x, label, "Messages Read");
        }
        ackEm(ctx, stats, ackList, 1);
        report(x, label, "Finished Reading Messages");
    }

    // ----------------------------------------------------------------------------------------------------
    // Pull
    // ----------------------------------------------------------------------------------------------------
    private static void subPull(Context ctx, Connection nc, Stats stats, int durableId, String label) throws Exception {
        String durable = ctx.getPullDurable(durableId);
        log("DURABLE " + durable);
        PullSubscribeOptions pso = ConsumerConfiguration.builder().durable(durable).ackPolicy(ctx.ackPolicy).buildPullSubscribeOptions();
        JetStream js = nc.jetStream();
        JetStreamSubscription sub = js.subscribe(ctx.subject, pso);
        if (ctx.pullTypeIterate) {
            _subPullIterate(ctx, stats, label, sub);
        }
        else {
            _subPullFetch(ctx, stats, label, sub);
        }
    }

    private static void _subPullFetch(Context ctx, Stats stats, String label, JetStreamSubscription sub) {
        int rcvd = 0;
        long hold = -1;
        List<Message> ackList = new ArrayList<>();
        while (ctx.subscribeCounter.get() < ctx.messageCount) {
            if (rcvd > 0) {
                stats.acceptHold(hold);
            }
            stats.start();
            List<Message> list = sub.fetch(ctx.batchSize, Duration.ofMillis(500));
            hold = stats.hold();
            for (Message m : list) {
                stats.count(m);
                ctx.subscribeCounter.incrementAndGet();
                ackMaybe(ctx, stats, ackList, m);
                reportMaybe(ctx, ++rcvd, label, "Messages Read");
            }
            if (rcvd == 0) {
                log("Waiting for first message.");
            }
        }
        stats.acceptHold(hold);
        ackEm(ctx, stats, ackList, 1);
        report(rcvd, label, "Finished Reading Messages");
    }

    private static void _subPullIterate(Context ctx, Stats stats, String label, JetStreamSubscription sub) {
        int rcvd = 0;
        long hold = -1;
        List<Message> ackList = new ArrayList<>();
        while (ctx.subscribeCounter.get() < ctx.messageCount) {
            if (rcvd > 0) {
                stats.acceptHold(hold);
            }
            stats.start();
            Iterator<Message> iter = sub.iterate(ctx.batchSize, Duration.ofMillis(500));
            hold = stats.hold();
            while (iter.hasNext()) {
                Message m = iter.next();
                stats.count(m);
                ctx.subscribeCounter.incrementAndGet();
                ackMaybe(ctx, stats, ackList, m);
                report(rcvd + 1, label, "-----");
                reportMaybe(ctx, ++rcvd, label, "Messages Read");
            }
            if (rcvd == 0) {
                log("Waiting for first message.");
            }
        }
        stats.acceptHold(hold);
        ackEm(ctx, stats, ackList, 1);
        report(rcvd, label, "Finished Reading Messages");
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------
    private static boolean isRegularTimeout(IOException ioe) {
        return ioe.getMessage().equals("Timeout or no response waiting for NATS JetStream server");
    }

    private static void ackMaybe(Context ctx, Stats stats, List<Message> ackList, Message m) {
        if (ctx.ackFrequency < 2) {
            stats.start();
            m.ack();
            stats.stop();
        }
        else {
            switch (ctx.ackPolicy) {
                case Explicit:
                case All:
                    ackList.add(m);
                    break;
            }
            ackEm(ctx, stats, ackList, ctx.ackFrequency);
        }
    }

    private static void ackEm(Context ctx, Stats stats, List<Message> ackList, int thresh) {
        if (ackList.size() >= thresh) {
            stats.start();
            switch (ctx.ackPolicy) {
                case Explicit:
                    for (Message m : ackList) {
                        m.ack();
                    }
                    break;
                case All:
                    ackList.get(ackList.size() - 1).ack();
                    break;
            }
            stats.stop();
            ackList.clear();
        }
    }

    private static void report(int x, String label, String message) {
        log(label + ": " + message + " " + Stats.format(x));
    }

    private static void reportMaybe(Context ctx, int x, String label, String message) {
        if (x % ctx.reportFrequency == 0) {
            report(x, label, message);
        }
    }

    private static void jitter(Context ctx) {
        if (ctx.jitter > 0) {
            sleep(ThreadLocalRandom.current().nextLong(ctx.jitter));
        }
    }

    private static Connection connect(Context ctx) throws Exception {
        Options options;
        if (ctx.optionsFactoryClassName == null) {
            options = defaultOptions(ctx.server);
        }
        else {
            Class<?> c = Class.forName(ctx.optionsFactoryClassName);
            Constructor<?> cons = c.getConstructor();
            OptionsFactory of = (OptionsFactory)cons.newInstance();
            options = of.getOptions();
        }
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
                }, ctx.action.getLabel() + id);
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
                    log("runIndividual " + nc);
                    runner.run(nc, stats, id);
                } catch (Exception e) {
                    logEx(e);
                }
            }, ctx.action.getLabel() + id);
            threads.add(t);
        }
        for (Thread t : threads) { t.start(); }
        for (Thread t : threads) { t.join(); }

        return statsList;
    }
}

// Copyright 2023 The NATS Authors
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

package io.nats;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class Rps implements ConnectionListener, ErrorListener {

    public static String STREAM = "rps-stream";
    public static String STREAM_SUBJECT = "rps-subject.>";
    public static String SUBJECT_1 = "rps-subject.1";
    public static String SUBJECT_2 = "rps-subject.2";
    public static final int PUBLISH_DELAY = 5000;

    // inactive threshold should be less than 3 times the idle heartbeat
    // 3 missed heartbeats causes the alarm
    // this config guarantees that the consumer will have idled out
    public static long IDLE_HEARTBEAT = 2000;
    public static long INACTIVE_THRESHOLD = 5500;

    static final List<AdvancedSubscription> SUBS = new ArrayList<>();
    private JetStream js;
    private Dispatcher dispatcher;

    public static void main(String[] args) {
        Rps rps = new Rps();
        Options options = Options.builder()
            .connectionListener(rps)
            .errorListener(rps)
            .build();

        try (Connection conn = Nats.connect(options)) {
            System.out.println("Connected to: " + conn.getServerInfo().getPort());
            setupStream(conn);
            JetStream js = conn.jetStream(); rps.js = js;

            Dispatcher dispatcher = conn.createDispatcher(); rps.dispatcher = dispatcher;

            AdvancedSubscription asub1 = new AdvancedSubscription(STREAM, SUBJECT_1);
            asub1.setSub(js.subscribe(null, dispatcher, asub1, false, asub1.getPso()));

            AdvancedSubscription asub2 = new AdvancedSubscription(STREAM, SUBJECT_2);
            asub2.setSub(js.subscribe(null, dispatcher, asub2, false, asub2.getPso()));

            // publish loop at the end also keeps the example app alive
            // inside the loop try/catch will track when a publish fails
            // since publishing will also be affected by disconnect
            int count1 = 0;
            int retry1 = 0;
            int count2 = 0;
            int retry2 = 0;
            //noinspection InfiniteLoopStatement
            while (true) {
                System.out.println("Attempting Publish on " + conn.getServerInfo().getPort());
                try {
                    ++count1;
                    js.publish(SUBJECT_1, ("S1-" + count1 + "-" + retry1).getBytes());
                    retry1 = 0;
                }
                catch (Exception e1) {
                    --count1;
                    retry1++;
                }
                try {
                    ++count2;
                    js.publish(SUBJECT_2, ("S2-" + count2 + "-" + retry2).getBytes());
                    retry2 = 0;
                }
                catch (Exception e2) {
                    --count2;
                    retry2++;
                }
                //noinspection BusyWait
                Thread.sleep(ThreadLocalRandom.current().nextLong(PUBLISH_DELAY));
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void heartbeatAlarm(Connection conn, JetStreamSubscription sub, long lastStreamSequence, long lastConsumerSequence) {
        for (AdvancedSubscription asub : SUBS) {
            if (asub.sub.equals(sub)) {
                System.out.println("heartbeatAlarm for " + sub.getConsumerName());
                // unsubscribe the old one, and ignore any error since it's probably already gone
                try {
                    asub.sub.unsubscribe();
                }
                catch (Exception ignore) {
                }

                // make a new subscription
                try {
                    asub.setSub(js.subscribe(null, dispatcher, asub, false, asub.getPso()));
                }
                catch (Exception e) {
                    // probably should do something here
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void connectionEvent(Connection conn, Events event) {
        System.out.println(event.name() + " " + conn.getServerInfo().getPort());
        switch (event) {
            case CONNECTED:
            case RECONNECTED:

            case CLOSED:
            case DISCONNECTED:
                // Might do something here, but heartbeat alarm covers a lot
                break;

            case LAME_DUCK:
                // trying to handle lame duck nicely. More work needed
                for (AdvancedSubscription asub : SUBS) {
                    try {
                        CompletableFuture<Boolean> future = asub.sub.drain(Duration.ofMillis(1000));
                        asub.drainFuture.set(future);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
        }
    }

    @Override
    public void errorOccurred(Connection conn, String error) {
        System.out.println("errorOccurred " + error);
    }

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {
        System.out.println("exceptionOccurred " + exp);
    }

    private static void setupStream(Connection conn) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = conn.jetStreamManagement();
        try {
            jsm.deleteStream(STREAM);
        }
        catch (Exception ignore) {}
        jsm.addStream(StreamConfiguration.builder().name(STREAM).subjects(STREAM_SUBJECT).storageType(StorageType.File).build());
    }
}

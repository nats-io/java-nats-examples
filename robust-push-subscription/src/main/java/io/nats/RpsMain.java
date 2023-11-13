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
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class RpsMain implements ConnectionListener, ErrorListener {

    public static String STREAM = "rps-stream";
    public static String STREAM_SUBJECT = "rps-subject.>";
    public static String SUBJECT_1 = "rps-subject.1";
    public static String SUBJECT_2 = "rps-subject.2";

    static final List<AdvancedSubscription> SUBS = new ArrayList<>();
    private JetStream js;
    private Dispatcher dispatcher;

    public static void main(String[] args) {
        RpsMain main = new RpsMain();
        Options options = Options.builder()
            .connectionListener(main)
            .errorListener(main)
            .build();

        try (Connection conn = Nats.connect(options)) {
            System.out.println("Connected to: " + conn.getServerInfo().getPort());
            setupStream(conn);
            JetStream js = conn.jetStream(); main.js = js;

            Dispatcher dispatcher = conn.createDispatcher(); main.dispatcher = dispatcher;

            AdvancedSubscription asub1 = new AdvancedSubscription();
            asub1.pso = getPso(SUBJECT_1, -1);
            asub1.sub = js.subscribe(null, dispatcher, asub1, false, asub1.pso);

            AdvancedSubscription asub2 = new AdvancedSubscription();
            asub2.pso = getPso(SUBJECT_2, -1);
            asub2.sub = js.subscribe(null, dispatcher, asub2, false, asub2.pso);

            // publish loop at the end also keeps the app running
            int mid = 0;
            while (true) {
                ++mid;
                js.publish(SUBJECT_1, ("S1-" + mid).getBytes());
                Thread.sleep(ThreadLocalRandom.current().nextLong(1000));
                js.publish(SUBJECT_2, ("S1-" + mid).getBytes());
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000));
            }

        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PushSubscribeOptions getPso(String subject, long lastSequence) {
        ConsumerConfiguration cc;
        if (lastSequence < 1) {
            cc = ConsumerConfiguration.builder()
                .filterSubject(subject)
                .flowControl(3000)
                .inactiveThreshold(10000)
                .build();
        }
        else {
            cc = ConsumerConfiguration.builder()
                .deliverPolicy(DeliverPolicy.ByStartSequence)
                .startSequence(lastSequence + 1)
                .filterSubject(subject)
                .flowControl(3000)
                .inactiveThreshold(10000)
                .build();
        }

        return PushSubscribeOptions.builder()
            .stream(STREAM)
            .configuration(cc)
            .build();
    }

    public void connectionEvent(Connection conn, Events event) {
        System.out.println(event);

        switch (event) {
            case CONNECTED:
                // mark all our trackers connected
                System.out.println("Connected to: " + conn.getServerInfo().getPort());
                for (AdvancedSubscription asub : SUBS) {
                    if (!asub.connected.get()) {
                        asub.connected.set(true);
                        // unsubscribe the old one
                        try {
                            asub.sub.unsubscribe();
                        }
                        catch (Exception ignore) {}

                        // make a new subscription
                        try {
                            asub.pso = getPso(SUBJECT_2, asub.lastAckedSequence.get());
                            asub.sub = js.subscribe(null, dispatcher, asub, false, asub.pso);
                        }
                        catch (Exception e) {
                            // probably should do something here
                            throw new RuntimeException(e);
                        }
                    }
                }
                break;

            case CLOSED:
            case DISCONNECTED:
                // mark all our trackers not connected
                for (AdvancedSubscription asub : SUBS) {
                    asub.connected.set(false);
                }
                break;

            case RECONNECTED:
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

    @Override
    public void heartbeatAlarm(Connection conn, JetStreamSubscription sub, long lastStreamSequence, long lastConsumerSequence) {
        for (AdvancedSubscription asub : SUBS) {
            if (asub.sub.equals(sub)) {
                asub.connected.set(false);
                break;
            }
        }
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

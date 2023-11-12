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

public class ConnectionLostWhilePullActive {
    static long EXPIRATION = 60000;
    public static void main(String[] args) {
        Options options = new Options.Builder()
            .server(Options.DEFAULT_URL)
            .connectionListener((conn, type) -> System.out.println("Connection Listener: " + type))
            .errorListener(new ExampleErrorListener())
            .build();

        try (Connection nc = Nats.connect(options)) {
            JetStream js = nc.jetStream();
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Create the stream.
            Utils.createTestStream(jsm);

            // Setup pull subscriptions. Could be durable, but not required for example.
            // Ephemerals that don't specify inactive threshold default to 5 seconds.
            JetStreamSubscription syncSub = js.subscribe(Utils.SUBJECT,
                PullSubscribeOptions.builder()
                    .name(Utils.SYNC_CONSUMER).build());

            Dispatcher d = nc.createDispatcher();
            JetStreamSubscription callbackSub = js.subscribe(Utils.SUBJECT, d, Message::ack,
                PullSubscribeOptions.builder()
                    .name(Utils.CALLBACK_CONSUMER).build());

            Thread publishThread = Utils.publishThread(js, 5000);
            publishThread.start();

            // Pull with long expiration.
            // No messages have been published to the subject,
            // so it will just wait to expire.
            PullRequestOptions pro = PullRequestOptions.builder(1000)
                .expiresIn(EXPIRATION)
                .idleHeartbeat(1000)
                .build();
            syncSub.pull(pro);
            callbackSub.pull(pro);

            System.out.println("\n" +
                "------------------------------------------------------------------------------------------------------\n" +
                "What happens when you kill and restart a server?\n" +
                "------------------------------------------------------------------------------------------------------\n" +
                "Experiment 1. Kill server with the consumer leader if it's not the same server you are connected to...\n" +
                "Look at the console for something like\n" +
                "    \"JetStream cluster new consumer leader for '$G > TheStream > SyncConsumer'\"\n" +
                "or use the NATS Cli 'nats c info' command\n" +
                "------------------------------------------------------------------------------------------------------\n" +
                "Experiment 2. Kill server you are connected to if it's not the same as the consumer leader...\n" +
                "------------------------------------------------------------------------------------------------------\n" +
                "Experiment 3. Kill server you are connected if it is the same as the consumer leader...\n" +
                "------------------------------------------------------------------------------------------------------\n"
            );

            // Both Sync and Callback subscriptions get messages sent to the error listener.
            // Sync subscriptions throw exceptions on errors.
            int x = 0;
            long time = System.currentTimeMillis();
            long stop = time + EXPIRATION + 1000;
            while (time < stop) {
                System.out.println("Sync Attempt " + (++x) + " @ " + time + " < " + stop);
                try {
                    Message m = syncSub.nextMessage(10000);
                    System.out.println("    " + m);
                    if (m != null) {
                        m.ack();
                    }
                }
                catch (JetStreamStatusException jsse) {
                    System.out.println("    Status Exception (" + x + ") " + jsse.getStatus());
                }
                catch (Exception e) {
                    System.out.println("    Exception (" + x + ") " + e);
                }
                time = System.currentTimeMillis();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

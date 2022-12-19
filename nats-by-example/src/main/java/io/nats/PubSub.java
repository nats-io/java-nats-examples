// Copyright 2022 The NATS Authors
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

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Core Publish-Subscribe in Messaging
 */
public class PubSub {

    public static void main(String[] args) {
        String natsURL = System.getenv("NATS_URL");
        if (natsURL == null) {
            natsURL = "nats://127.0.0.1:4222";
        }

        // Initialize a connection to the server. The connection is AutoCloseable
        // on exit.
        try (Connection nc = Nats.connect(natsURL)) {

            // Prepare a simple message body and publish a message to a subject.
            byte[] messageBytes = "hello".getBytes(StandardCharsets.UTF_8);
            nc.publish("greet.joe", messageBytes);

            // Create a message dispatcher for handling messages in a
            // separate thread and then subscribe to the target subject
            // which leverages a wildcard `greet.*`.
            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                System.out.printf("%s on subject %s\n",
                    new String(msg.getData(), StandardCharsets.UTF_8),
                    msg.getSubject());
            });

            dispatcher.subscribe("greet.*");

            // Publish more messages that will be received by the subscription
            // since they match the wildcard. Note the first message on
            // `greet.joe` was not received because we were not subscribed when
            // it was published
            nc.publish("greet.bob", messageBytes);
            nc.publish("greet.sue", messageBytes);
            nc.publish("greet.pam", messageBytes);

            // Sleep this thread a little so the dispatcher thread has time
            // to receive all the messages before the program quits.
            Thread.sleep(200);

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}

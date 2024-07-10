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
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrate confirmed ack, AKA ack sync, ack ack or double ack
 */
public class AckVerify {
    public static void main(String[] args) {
        String natsURL = System.getenv("NATS_URL");
        if (natsURL == null) {
            natsURL = "nats://127.0.0.1:4222";
        }

        try (Connection nc = Nats.connect(natsURL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();
            JetStream js = jsm.jetStream();

            // Create a stream
            // (remove the stream so we have a clean starting point)
            try { jsm.deleteStream("verifyAckStream"); } catch (JetStreamApiException e) {}
            jsm.addStream(StreamConfiguration.builder()
                .name("verifyAckStream")
                .subjects("verifyAckSubject")
                .storageType(StorageType.Memory)
                .build());

            // Publish a couple messages so we can look at the state
            js.publish("verifyAckSubject", "A".getBytes());
            js.publish("verifyAckSubject", "B".getBytes());

            // Consume a message with 2 different consumers
            // The first consumer will ack without confirmation
            // The second consumer will ackSync which confirms that ack was handled.
            StreamContext sc = nc.getStreamContext("verifyAckStream");
            ConsumerContext cc1 = sc.createOrUpdateConsumer(ConsumerConfiguration.builder().filterSubject("verifyAckSubject").build());
            ConsumerContext cc2 = sc.createOrUpdateConsumer(ConsumerConfiguration.builder().filterSubject("verifyAckSubject").build());

            // Consumer 1
            ConsumerInfo ci = cc1.getConsumerInfo();
            System.out.println("Consumer 1");
            System.out.println("  Start\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());

            Message m = cc1.next();
            ci = cc1.getConsumerInfo();
            System.out.println("  After received but before ack\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());

            m.ack();
            Thread.sleep(100); // to give time for the ack to be completed on the server
            ci = cc1.getConsumerInfo();
            System.out.println("  After ack\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());

            // Consumer 2
            ci = cc2.getConsumerInfo();
            System.out.println("Consumer 2");
            System.out.println("  Start\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());

            m = cc2.next();
            ci = cc2.getConsumerInfo();
            System.out.println("  After received but before ack\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());

            m.ackSync(Duration.ofMillis(500));
            ci = cc2.getConsumerInfo();
            System.out.println("  After ack\n    # pending messages: " + ci.getNumPending() + "\n    # messages with ack pending: " + ci.getNumAckPending());
        }
        catch (InterruptedException | IOException | JetStreamApiException | JetStreamStatusCheckedException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}

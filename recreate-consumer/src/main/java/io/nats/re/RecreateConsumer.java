package io.nats.re;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.DeliverPolicy;

public class RecreateConsumer {

    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            ConsumerInfo ci = jsm.getConsumerInfo("re-stream", "original-durable-name");
            long lastSeq = ci.getAckFloor().getStreamSequence();
            System.out.println("The last acknowledged message's stream sequence is: " + lastSeq + "\n");

            // We are done with the original consumer so we can get rid of it.
            jsm.deleteConsumer("re-stream", "original-durable-name");

            ConsumerConfiguration cc = ConsumerConfiguration
                .builder(ci.getConsumerConfiguration())         // base the new consumer on the old consumer
                .durable("updated-durable-name")                // but change the durable name
                .deliverSubject("updated-deliver-subject")      // change the deliver subject just to be safe
                .deliverPolicy(DeliverPolicy.ByStartSequence)   // setup the consumer to start at a specific stream sequence
                .startSequence(lastSeq + 1)                     // tell the consumer which stream sequence
                                                                // ... change anything else you want
                .build();

            jsm.addOrUpdateConsumer("re-stream", cc);

            JetStream js = nc.jetStream();
            PushSubscribeOptions pso = PushSubscribeOptions.bind("re-stream", "updated-durable-name");
            JetStreamSubscription sub = js.subscribe("re-sub", pso);

            Message m = sub.nextMessage(1000);
            while (m != null) {
                m.ack();
                System.out.println("Read Message, Stream Sequence: " + m.metaData().streamSequence() + ", Data: " + new String(m.getData()));
                m = sub.nextMessage(1000);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

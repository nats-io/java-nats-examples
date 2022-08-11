package io.nats.re;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;

import java.time.Duration;

public class MakeConsumer {

    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                .durable("original-durable-name")
                .deliverSubject("original-deliver-subject")
                .filterSubject("re-sub")
                .maxAckPending(5) // prevents server from sending all of the messages
                                  // at once since this example stops before finishing reading
                .ackWait(Duration.ofSeconds(3)) // keeping short for example only
                .build();
            jsm.addOrUpdateConsumer("re-stream", cc);

            JetStream js = nc.jetStream();
            PushSubscribeOptions pso = PushSubscribeOptions.bind("re-stream", "original-durable-name");
            JetStreamSubscription sub = js.subscribe("re-sub", pso);

            // only read 10 of the 20
            for (int x = 1; x <= 10; x++) {
                Message m = sub.nextMessage(1000);
                m.ack();
                System.out.println("Read Message, Stream Sequence: " + m.metaData().streamSequence() + ", Data: " + new String(m.getData()));
            }
            sub.unsubscribe();

            Thread.sleep(5000); // just give the server time to make sure the durable reflects the actual ack state.

            ConsumerInfo ci = jsm.getConsumerInfo("re-stream", "original-durable-name");
            long lastSeq = ci.getAckFloor().getStreamSequence();
            System.out.println("\nThe last acknowledged message's stream sequence is: " + lastSeq);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

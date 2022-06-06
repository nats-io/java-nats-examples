package io.nats.jsovercore;

import io.nats.client.*;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import static io.nats.jsovercore.OverUtils.createStream;

class JsPublishOverCore {
    static final String STREAM = "pubover";
    static final String DATA_SUBJECT = "pub-data-sub";
    static final String PUB_ACK_SUBJECT = "pa-sub";
    static final int MSG_COUNT = 10;

    public static void main(String[] args) {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            createStream(nc, STREAM, DATA_SUBJECT);

            // Create a core subscription to listen on the pub_ack subject
            // You might do this in a completely different thread
            // and it could also be done with a synchronous subscribe
            // ! As long as it's started before publishing starts !

            Dispatcher dispatcher = nc.createDispatcher();
            CountDownLatch pubAckLatch = new CountDownLatch(MSG_COUNT);
            MessageHandler pubAckHandler = msg -> {
                try {
                    // you can read the message yourself or convert it to a PublishAck object
                    PublishAck pa = new PublishAck(msg);
                    System.out.println(pa);
                } catch (Exception e) {
                    // creation of pub ack will throw an exception if there was an error publishing
                    System.out.println(e);
                }
                pubAckLatch.countDown();
            };
            dispatcher.subscribe(PUB_ACK_SUBJECT, pubAckHandler);

            // core publish to the subject. The reply-to is the key.
            // since this subject is a jetstream subject the server
            // "replies" and sends the Publish Ack to the reply-to
            for (int x = 1; x <= MSG_COUNT; x++) {
                Message msg = NatsMessage.builder()
                    .subject(DATA_SUBJECT)
                    .replyTo(PUB_ACK_SUBJECT)
                    .data("PubOverCore-" + x, StandardCharsets.UTF_8)
                    .build();
                nc.publish(msg);
            }

            pubAckLatch.await();

            JetStreamSubscription sub = nc.jetStream().subscribe(DATA_SUBJECT);
            for (int x = 1; x <= MSG_COUNT; x++) {
                Message msg = sub.nextMessage(1000);
                System.out.println("Subject '" + msg.getSubject() + "' Data: '" + new String(msg.getData()) + "' StreamSeq:" + msg.metaData().streamSequence());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}

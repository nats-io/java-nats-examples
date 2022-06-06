package io.nats.jsovercore;

import io.nats.client.*;
import io.nats.client.impl.NatsMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import static io.nats.jsovercore.OverUtils.createStream;

class JsSubscribeOverCore {
    static final String STREAM = "subover";
    static final String DATA_SUBJECT = "sub-data-sub";
    static final int MSG_COUNT = 10;

    public static void main(String[] args) {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            createStream(nc, STREAM, DATA_SUBJECT);

            // SINCE WE ARE USING CORE, ALL SUBSCRIPTIONS MUST BE STARTED BEFORE ANY PUBLISHES

            // create a dispatcher
            Dispatcher dispatcher = nc.createDispatcher();
            CountDownLatch subLatch = new CountDownLatch(MSG_COUNT);
            MessageHandler subHandler = msg -> {
                // This message will NOT have meta data.
                // You also cannot ack it.
                System.out.println("MSG Subject '" + msg.getSubject() + "' Data: '" + new String(msg.getData()) + "'");
                subLatch.countDown();
            };
            dispatcher.subscribe(DATA_SUBJECT, subHandler);

            JetStream js = nc.jetStream();
            for (int x = 1; x <= MSG_COUNT; x++) {
                Message msg = NatsMessage.builder()
                    .subject(DATA_SUBJECT)
                    .data("SubOverCore-" + x, StandardCharsets.UTF_8)
                    .build();
                System.out.println(js.publish(msg));
            }
            subLatch.await();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}

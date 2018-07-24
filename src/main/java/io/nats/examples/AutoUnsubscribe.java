package io.nats.examples;

import java.nio.charset.StandardCharsets;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class AutoUnsubscribe {
    public static void main(String[] args) {

        try {
            // [begin unsubscribe_auto]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");
            Dispatcher d = nc.createDispatcher((msg) -> {
                String str = new String(msg.getData(), StandardCharsets.UTF_8);
                System.out.println(str);
            });

            // Sync Subscription
            Subscription sub = nc.subscribe("updates");
            sub.unsubscribe(1);

            // Async Subscription
            d.subscribe("updates");
            d.unsubscribe("updates", 1);

            // Close the connection
            nc.close();
            // [end unsubscribe_auto]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
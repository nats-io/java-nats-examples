package io.nats.examples;

import java.nio.charset.StandardCharsets;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class Unsubscribe {
    public static void main(String[] args) {

        try {
            // [begin unsubscribe]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");
            Dispatcher d = nc.createDispatcher((msg) -> {
                String str = new String(msg.getData(), StandardCharsets.UTF_8);
                System.out.println(str);
            });

            // Sync Subscription
            Subscription sub = nc.subscribe("updates");
            sub.unsubscribe();

            // Async Subscription
            d.subscribe("updates");
            d.unsubscribe("updates");

            // Close the connection
            nc.close();
            // [end unsubscribe]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
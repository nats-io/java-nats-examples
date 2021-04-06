package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class SetPendingLimits {
    public static void main(String[] args) {

        try {
            // [begin slow_pending_limits]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");
            
            Dispatcher d = nc.createDispatcher((msg) -> {
                // do something
            });

            d.subscribe("updates");

            d.setPendingLimits(1_000, 5 * 1024 * 1024); // Set limits on a dispatcher

            // Subscribe
            Subscription sub = nc.subscribe("updates");

            sub.setPendingLimits(1_000, 5 * 1024 * 1024); // Set limits on a subscription

            // Do something

            // Close the connection
            nc.close();
            // [end slow_pending_limits]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
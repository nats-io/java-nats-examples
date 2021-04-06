package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class SubscribeAsync {
    public static void main(String[] args) {

        try {
            // [begin subscribe_async]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Use a latch to wait for a message to arrive
            CountDownLatch latch = new CountDownLatch(1);

            // Create a dispatcher and inline message handler
            Dispatcher d = nc.createDispatcher((msg) -> {
                String str = new String(msg.getData(), StandardCharsets.UTF_8);
                System.out.println(str);
                latch.countDown();
            });

            // Subscribe
            d.subscribe("updates");

            // Wait for a message to come in
            latch.await(); 

            // Close the connection
            nc.close();
            // [end subscribe_async]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
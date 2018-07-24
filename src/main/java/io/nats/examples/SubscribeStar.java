package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class SubscribeStar {
    public static void main(String[] args) {

        try {
            // [begin subscribe_star]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Use a latch to wait for 2 messages to arrive
            CountDownLatch latch = new CountDownLatch(2);

            // Create a dispatcher and inline message handler
            Dispatcher d = nc.createDispatcher((msg) -> {
                String subject = msg.getSubject();
                String str = new String(msg.getData(), StandardCharsets.UTF_8);
                System.out.println(subject + ": " + str);
                latch.countDown();
            });

            // Subscribe
            d.subscribe("time.*.east");

            // Wait for messages to come in
            latch.await();

            // Close the connection
            nc.close();
            // [end subscribe_star]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class SubscribeArrow {
    public static void main(String[] args) {

        try {
            // [begin subscribe_arrow]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Use a latch to wait for 4 messages to arrive
            CountDownLatch latch = new CountDownLatch(4);

            // Create a dispatcher and inline message handler
            Dispatcher d = nc.createDispatcher((msg) -> {
                String subject = msg.getSubject();
                String str = new String(msg.getData(), StandardCharsets.UTF_8);
                System.out.println(subject + ": " + str);
                latch.countDown();
            });

            // Subscribe
            d.subscribe("time.>");

            // Wait for messages to come in
            latch.await();

            // Close the connection
            nc.close();
            // [end subscribe_arrow]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
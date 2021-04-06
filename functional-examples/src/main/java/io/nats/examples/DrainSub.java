package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class DrainSub {
    public static void main(String[] args) {

        try {
            // [begin drain_sub]
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

            // Messages that have arrived will be processed
            CompletableFuture<Boolean> drained = d.drain(Duration.ofSeconds(10));

            // Wait for the drain to complete
            drained.get();

            // Close the connection
            nc.close();
            // [end drain_sub]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
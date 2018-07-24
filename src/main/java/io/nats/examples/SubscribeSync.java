package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class SubscribeSync {
    public static void main(String[] args) {

        try {
            // [begin subscribe_sync]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Subscribe
            Subscription sub = nc.subscribe("updates");

            // Read a message
            Message msg = sub.nextMessage(Duration.ZERO);

            String str = new String(msg.getData(), StandardCharsets.UTF_8);
            System.out.println(str);

            // Close the connection
            nc.close();
            // [end subscribe_sync]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class Flush {
    public static void main(String[] args) {

        try {
            // [begin flush]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            nc.publish("updates", "All is Well".getBytes(StandardCharsets.UTF_8));
            nc.flush(Duration.ofSeconds(1)); // Flush the message queue

            nc.close();
            // [end flush]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class PublishBytes {
    public static void main(String[] args) {

        try {
            // [begin publish_bytes]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            nc.publish("updates", "All is Well".getBytes(StandardCharsets.UTF_8));

            // Make sure the message goes through before we close
            nc.flush(Duration.ZERO);
            nc.close();
            // [end publish_bytes]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
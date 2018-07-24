package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.NUID;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class PublishWithReply {
    public static void main(String[] args) {

        try {
            // [begin publish_with_reply]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Create a unique subject name
            String uniqueReplyTo = NUID.nextGlobal();

            // Listen for a single response
            Subscription sub = nc.subscribe(uniqueReplyTo);
            sub.unsubscribe(1);

            // Send the request
            nc.publish("time", uniqueReplyTo, null);

            Message msg = sub.nextMessage(Duration.ofSeconds(1));

            // Use the response
            System.out.println(new String(msg.getData(), StandardCharsets.UTF_8));

            // Close the connection
            nc.close();
            // [end publish_with_reply]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
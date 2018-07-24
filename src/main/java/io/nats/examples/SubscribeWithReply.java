package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;

public class SubscribeWithReply {
    public static void main(String[] args) {

        try {
            // [begin subscribe_w_reply]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Subscribe
            Subscription sub = nc.subscribe("time");

            // Read a message
            Message msg = sub.nextMessage(Duration.ZERO);

            // Get the time
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            byte[] timeAsBytes = sdf.format(cal.getTime()).getBytes(StandardCharsets.UTF_8);

            // Send the time
            nc.publish(msg.getReplyTo(), timeAsBytes);

            // Flush and close the connection
            nc.flush(Duration.ZERO);
            nc.close();
            // [end subscribe_w_reply]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
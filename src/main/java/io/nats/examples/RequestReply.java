package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;

public class RequestReply {
    public static void main(String[] args) {

        try {
            // [begin request_reply]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Send the request
            Message msg = nc.request("time", null, Duration.ofSeconds(1));

            // Use the response
            System.out.println(new String(msg.getData(), StandardCharsets.UTF_8));

            // Close the connection
            nc.close();
            // [end request_reply]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
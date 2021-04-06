package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class GetMaxPayload {
    public static void main(String[] args) {

        try {
            // [begin max_payload]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            long max = nc.getMaxPayload();
            // Do something with the max payload

            nc.close();
            // [end max_payload]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
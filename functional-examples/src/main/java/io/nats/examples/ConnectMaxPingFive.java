package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectMaxPingFive {
    public static void main(String[] args) {

        try {
            // [begin ping_5]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        maxPingsOut(5). // Set max pings in flight
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end ping_5]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
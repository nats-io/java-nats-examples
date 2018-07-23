package io.nats.examples;

import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectPingTwenty {
    public static void main(String[] args) {

        try {
            // [begin ping_20s]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        pingInterval(Duration.ofSeconds(20)). // Set Ping Interval
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end ping_20s]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
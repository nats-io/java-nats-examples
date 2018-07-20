package io.nats.examples;

import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ReconnectTenSeconds {
    public static void main(String[] args) {

        try {
            // [begin reconnect_10s]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        reconnectWait(Duration.ofSeconds(10)).  // Set Reconnect Wait
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end reconnect_10s]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
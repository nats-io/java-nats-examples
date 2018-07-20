package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ReconnectFiveMB {
    public static void main(String[] args) {

        try {
            // [begin reconnect_5mb]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        reconnectBufferSize(5 * 1024 * 1024).  // Set buffer in bytes
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end reconnect_5mb]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
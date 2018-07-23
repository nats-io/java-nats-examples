package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectPedantic {
    public static void main(String[] args) {

        try {
            // [begin connect_pedantic]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        pedantic(). // Turn on pedantic
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_pedantic]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
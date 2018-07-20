package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectWithOptions {
    public static void main(String[] args) {

        try {
            // [begin connect_options]
            Options options = new Options.Builder().
                                            server("nats://localhost:1222").
                                            noReconnect().
                                            build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_options]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
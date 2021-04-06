package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectVerbose {
    public static void main(String[] args) {

        try {
            // [begin connect_verbose]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        verbose(). // Turn on verbose
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_verbose]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectName {
    public static void main(String[] args) {

        try {
            // [begin connect_name]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        connectionName("my-connection"). // Set a connection name
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_name]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
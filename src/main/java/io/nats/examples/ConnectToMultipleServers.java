package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectToMultipleServers {
    public static void main(String[] args) {

        try {
            // [begin connect_multiple]
            Options options = new Options.Builder().
                                        server("nats://localhost:1222").
                                        server("nats://localhost:1223").
                                        server("nats://localhost:1224").
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_multiple]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
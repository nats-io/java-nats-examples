package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectToken {
    public static void main(String[] args) {

        try {
            // [begin connect_token]
            Options options = new Options.Builder().
                                        server("nats://localhost:4222").
                                        token("mytoken"). // Set a token
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_token]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
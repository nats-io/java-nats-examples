package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectCreds {
    public static void main(String[] args) {

        try {
            // [begin connect_creds]
            Options options = new Options.Builder().
                        server("nats://localhost:4222").
                        authHandler(Nats.credentials("path_to_creds_file")).
                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_creds]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
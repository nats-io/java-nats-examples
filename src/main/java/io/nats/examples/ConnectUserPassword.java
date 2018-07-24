package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectUserPassword {
    public static void main(String[] args) {

        try {
            // [begin connect_userpass]
            Options options = new Options.Builder().
                                        server("nats://localhost:4222").
                                        userInfo("myname","password"). // Set a user and plain text password
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_userpass]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
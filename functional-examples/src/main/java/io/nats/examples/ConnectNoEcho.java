package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectNoEcho {
    public static void main(String[] args) {

        try {
            // [begin no_echo]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        noEcho(). // Turn off echo
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end no_echo]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
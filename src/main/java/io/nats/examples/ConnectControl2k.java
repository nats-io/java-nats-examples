package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ConnectControl2k {
    public static void main(String[] args) {

        try {
            // [begin control_2k]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        maxControlLine(2 * 1024). // Set the max control line to 2k
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end control_2k]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
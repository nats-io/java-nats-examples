package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectToDefaultServer {
    public static void main(String[] args) {

        try {
            // [begin connect_default]
            Connection nc = Nats.connect();

            // Do something with the connection

            nc.close();
            // [end connect_default]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectToDefaultServer {
    public static void main(String[] args) {

        try {
            // [begin connect_default]
            Connection nc = Nats.connect();
            nc.close();
            // [end connect_default]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
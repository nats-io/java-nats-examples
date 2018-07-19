package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectToDefaultServer {
    public static void main(String[] args) {

        try {
            // [begin default_connect]
            Connection nc = Nats.connect();
            nc.close();
            // [end default_connect]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
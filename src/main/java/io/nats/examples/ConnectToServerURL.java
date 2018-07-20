package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectToServerURL {
    public static void main(String[] args) {

        try {
            // [begin connect_url]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");
            nc.close();
            // [end connect_url]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
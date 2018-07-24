package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectUserPasswordURL {
    public static void main(String[] args) {

        try {
            // [begin connect_userpass_url]
            Connection nc = Nats.connect("nats://myname:password@localhost:4222");

            // Do something with the connection

            nc.close();
            // [end connect_userpass_url]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class ConnectTokenURL {
    public static void main(String[] args) {

        try {
            // [begin connect_token_url]
            Connection nc = Nats.connect("nats://mytoken@localhost:4222");//Token in URL

            // Do something with the connection

            nc.close();
            // [end connect_token_url]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class GetConnectionStatus {
    public static void main(String[] args) {

        try {
            // [begin connect_status]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            System.out.println("The Connection is: " + nc.getStatus());

            nc.close();
            
            System.out.println("The Connection is: " + nc.getStatus());
            // [end connect_status]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
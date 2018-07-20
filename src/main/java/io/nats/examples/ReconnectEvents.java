package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.ConnectionListener.Events;

public class ReconnectEvents {
    public static void main(String[] args) {

        try {
            // [begin reconnect_10x]
            Options options = new Options.Builder().
                                        server("nats://demo.nats.io:4222").
                                        connectionListener((conn, type) -> {
                                            if (type == Events.RECONNECTED) {
                                                // handle reconnected
                                            } else if (type == Events.DISCONNECTED) {
                                                // handle disconnected, wait for reconnect
                                            }
                                        }).
                                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end reconnect_10x]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
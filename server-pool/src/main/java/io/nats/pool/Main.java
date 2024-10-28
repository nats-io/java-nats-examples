package io.nats.pool;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class Main {
    static String[] BOOTSTRAP = new String[] {"nats://host1:4222","nats://host2:4222","nats://host3:4222"};

    public static void main(String[] args) {
        // provide a custom implementation of the ServerPool
        // There also is a property for a no-op constructor ServerPool instance: servers_pool_implementation_class
        Options options = new Options.Builder()
            .servers(BOOTSTRAP)
            .serverPool(new ExampleServerPool())
            .build();

        try (Connection nc = Nats.connect(options)) {
            // During connects or reconnects, the server pool implementation is called
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

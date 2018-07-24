package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import com.google.gson.Gson;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

// [begin subscribe_json]
class StockForJsonSub {
    public String symbol;
    public float price;

    public String toString() {
        return symbol + " is at " + price;
    }
}

public class SubscribeJSON {
    public static void main(String[] args) {

        try {
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            // Use a latch to wait for 10 messages to arrive
            CountDownLatch latch = new CountDownLatch(10);

            // Create a dispatcher and inline message handler
            Dispatcher d = nc.createDispatcher((msg) -> {
                Gson gson = new Gson();

                String json = new String(msg.getData(), StandardCharsets.UTF_8);
                StockForJsonSub stk = gson.fromJson(json, StockForJsonSub.class);
                
                // Use the object
                System.out.println(stk);

                latch.countDown();
            });

            // Subscribe
            d.subscribe("updates");

            // Wait for a message to come in
            latch.await(); 

            // Close the connection
            nc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// [end subscribe_json]
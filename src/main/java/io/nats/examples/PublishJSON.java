package io.nats.examples;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.nats.client.Connection;
import io.nats.client.Nats;

// [begin publish_json]
class StockForJson {
    public String symbol;
    public float price;
}

public class PublishJSON {
    public static void main(String[] args) {
        try {
            Connection nc = Nats.connect("nats://demo.nats.io:4222");

            StockForJson stk = new StockForJson();
            stk.symbol="GOOG";
            stk.price=1200;

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String json = gson.toJson(stk);
            nc.publish("updates", json.getBytes(StandardCharsets.UTF_8));

            nc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// [end publish_json]
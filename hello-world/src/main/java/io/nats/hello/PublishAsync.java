package io.nats.hello;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/*
Asynchronous Publish Example using the NATS client for Java
 */
public class PublishAsync
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost")) {
            JetStream js = nc.jetStream();

            List<CompletableFuture<PublishAck>> futures = new ArrayList<>();
            for (int x = 1; x <= 100; x++) {
                System.out.println("About to publish " + x);
                CompletableFuture<PublishAck> future = js.publishAsync("subject", ("data"+x).getBytes());
                futures.add(future);
            }

            for (CompletableFuture<PublishAck> future : futures) {
                PublishAck pa = future.get(1, TimeUnit.SECONDS);
                System.out.println(pa);
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}

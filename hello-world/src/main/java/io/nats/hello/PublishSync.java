package io.nats.hello;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;

/*
Synchronous Publish Example using the NATS client for Java
 */
public class PublishSync
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost")) {
            JetStream js = nc.jetStream();

            for (int x = 1; x <= 100; x++) {
                System.out.println("About to publish " + x);
                PublishAck pa = js.publish("subject", ("data"+x).getBytes());
                System.out.println(pa);
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}

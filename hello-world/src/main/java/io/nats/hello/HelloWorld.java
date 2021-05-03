package io.nats.hello;

import io.nats.client.*;
import io.nats.client.api.PublishAck;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;

import java.time.Duration;

/*
JetStream Hello World Example using the NATS client for Java
 */
public class HelloWorld
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("hello")
                    .subjects("world")
                    .storageType(StorageType.Memory)
                    .build();

            // Create the stream
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            JsonUtils.printFormatted(streamInfo);

            JetStream js = nc.jetStream();
            PublishAck ack = js.publish("world", "one".getBytes());
            JsonUtils.printFormatted(ack);

            ack = js.publish("world", "two".getBytes());
            JsonUtils.printFormatted(ack);

            JetStreamSubscription sub = js.subscribe("world");
            Message m = sub.nextMessage(Duration.ofSeconds(3));
            m.ack();
            System.out.println("Message: " + m.getSubject() + " " + new String(m.getData()));
            JsonUtils.printFormatted(m.metaData());

            m = sub.nextMessage(Duration.ofSeconds(3));
            m.ack();
            System.out.println("Message: " + m.getSubject() + " " + new String(m.getData()));
            JsonUtils.printFormatted(m.metaData());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package io.nats.hello;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;

import java.time.Duration;

public class StreamWildcardSubSpecfic
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("hello")
                    .storageType(StorageType.Memory)
                    .subjects("world.*")
                    .build();

            // Create the stream
            StreamInfo streamInfo = jsm.addStream(streamConfig);

            JsonUtils.printFormatted(streamInfo);

            JetStream js = nc.jetStream();
            js.publish("world.one", "one-data".getBytes());
            js.publish("world.two", "two-data".getBytes());

            JetStreamSubscription sub = js.subscribe("world.one");
            Message m = sub.nextMessage(Duration.ofSeconds(1));
            System.out.println("Message: " + m.getSubject() + " " + new String(m.getData()));
            JsonUtils.printFormatted(m.metaData());

            sub = js.subscribe("world.two");
            m = sub.nextMessage(Duration.ofSeconds(1));
            System.out.println("Message: " + m.getSubject() + " " + new String(m.getData()));
            JsonUtils.printFormatted(m.metaData());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

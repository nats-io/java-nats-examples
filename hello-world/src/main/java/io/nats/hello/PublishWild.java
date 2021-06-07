package io.nats.hello;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;

/*
Wildcard Subject Example using the NATS client for Java
 */
public class PublishWild
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration and create the stream
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("stream")
                    .subjects("hierarchy.one.A", "hierarchy.one.B", "hierarchy.two.B")
//                    .subjects("hierarchy.sub.*")
//                    .subjects("hierarchy.*")
//                    .subjects("hierarchy.*.A")
//                    .subjects("hierarchy.sub.>")
//                    .subjects("hierarchy.>")
                    .storageType(StorageType.Memory)
                    .build();
            StreamInfo si = jsm.addStream(streamConfig);
            System.out.println("Subject(s): " + si.getConfiguration().getSubjects());

            JetStream js = nc.jetStream();

            publish(js, "hierarchy.sub.A");
            publish(js, "hierarchy.sub.B");
            publish(js, "hierarchy.sub.C");
            publish(js, "hierarchy.D");
            publish(js, "hierarchy.baz.A");
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void publish(JetStream js, String subject) {
        try {
            System.out.println("\nTrying to publish to " + subject);
            PublishAck ack = js.publish(subject, null);
            JsonUtils.printFormatted(ack);
        }
        catch (Exception e) {
            System.out.println("failed b/c " + e);
        }
    }
}

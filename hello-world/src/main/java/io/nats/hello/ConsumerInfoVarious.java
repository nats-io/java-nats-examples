package io.nats.hello;

import io.nats.client.*;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.time.Duration;
import java.util.List;

import static io.nats.client.support.JsonUtils.printFormatted;

/*
Demonstrate various ways to get consumer info
 */
public class ConsumerInfoVarious
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration and create the stream
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("stream")
                    .subjects("subject")
                    .storageType(StorageType.Memory)
                    .build();

            jsm.addStream(streamConfig);

            JetStream js = nc.jetStream();

            js.publish("subject", "data1".getBytes());
            js.publish("subject", "data2".getBytes());
            js.publish("subject", "data3".getBytes());
            js.publish("subject", "data4".getBytes());
            js.publish("subject", "data5".getBytes());
            js.publish("subject", "data6".getBytes());
            js.publish("subject", "data7".getBytes());
            js.publish("subject", "data8".getBytes());
            js.publish("subject", "data9".getBytes());

            JetStreamSubscription sub1 = js.subscribe("subject");
            ConsumerInfo ci1 = sub1.getConsumerInfo();
            System.out.println("CONSUMER 1");
            printFormatted(ci1);

            JetStreamSubscription sub2 = js.subscribe("subject");
            ConsumerInfo ci2 = sub2.getConsumerInfo();
            System.out.println("\nCONSUMER 2");
            printFormatted(ci2);

            sub1.nextMessage(Duration.ofSeconds(1)).ack();
            sub1.nextMessage(Duration.ofSeconds(1)).ack();
            sub1.nextMessage(Duration.ofSeconds(1)).ack();
            sub1.nextMessage(Duration.ofSeconds(1)).ack();
            sub1.nextMessage(Duration.ofSeconds(1)).ack();

            sub2.nextMessage(Duration.ofSeconds(1)).ack();
            sub2.nextMessage(Duration.ofSeconds(1)).ack();
            sub2.nextMessage(Duration.ofSeconds(1)).ack();

            List<ConsumerInfo> list = jsm.getConsumers("stream");
            System.out.println("\nCONSUMER LIST");
            for (ConsumerInfo ci : list) {
                printFormatted(ci);
            }

            Message m = sub1.nextMessage(Duration.ofSeconds(1));
            m.ack();
            System.out.println("\nCONSUMER 1 MESSAGE META DATA");
            printFormatted(m.metaData());
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}

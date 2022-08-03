package io.nats.msw;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

public class Publisher
{
    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            try { jsm.deleteStream("msw-one"); } catch (Exception ignore) {}
            try { jsm.deleteStream("msw-two"); } catch (Exception ignore) {}

            StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name("msw-stream-one")
                .subjects("msw.one.>")
                .storageType(StorageType.Memory)
                .build();
            jsm.addStream(streamConfig);

            streamConfig = StreamConfiguration.builder()
                .name("msw-stream-two")
                .subjects("msw.two.>")
                .storageType(StorageType.Memory)
                .build();
            jsm.addStream(streamConfig);

            for (int x = 10000; x < 20000; x++) {
                // fastest publish, don't care about acks, this is assuming there will be no error
                nc.publish("msw.one.EAST.segment", ("E-one-" + x).getBytes());
                nc.publish("msw.one.WEST.segment", ("W-one" + x).getBytes());
                nc.publish("msw.two.EAST.segment", ("E-two-" + x).getBytes());
                nc.publish("msw.two.WEST.segment", ("W-two-" + x).getBytes());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

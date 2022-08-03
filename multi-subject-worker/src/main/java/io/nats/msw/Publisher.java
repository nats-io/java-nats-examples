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

            try { jsm.deleteStream("msw-stream"); } catch (Exception ignore) {}
            
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name("msw-stream")
                .subjects("msw.>")
                .storageType(StorageType.Memory)
                .build();
            jsm.addStream(streamConfig);

            for (int x = 10000; x < 20000; x++) {
                // fastest publish, don't care about acks, this is assuming there will be no error
                nc.publish("msw.A.segment", ("A-" + x).getBytes());
                nc.publish("msw.B.segment", ("B-" + x).getBytes());
                nc.publish("msw.C.segment", ("C-" + x).getBytes());
                nc.publish("msw.D.segment", ("D-" + x).getBytes());
                nc.publish("msw.E.segment", ("E-" + x).getBytes());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package io.nats.re;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import static io.nats.client.support.JsonUtils.printFormatted;

public class Publisher
{
    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            try { jsm.deleteStream("re-stream"); } catch (Exception ignore) {}

            StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name("re-stream")
                .subjects("re-sub")
                .storageType(StorageType.Memory)
                .build();
            jsm.addStream(streamConfig);

            for (int x = 1; x <= 20; x++) {
                // fastest publish, don't care about acks, this is assuming there will be no error
                nc.publish("re-sub", ("re-msg-" + x).getBytes());
            }

            printFormatted(jsm.getStreamInfo("re-stream").getStreamState());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

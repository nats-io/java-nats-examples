package io.nats.jsovercore;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;

class OverUtils {

    public static void createStream(Connection nc, String stream, String... subjects) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = nc.jetStreamManagement();

        try {
            jsm.deleteStream(stream);
        } catch (JetStreamApiException e) {
            // ignore for delete means didn't exist
        }

        // Build the configuration and create the stream
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .name(stream)
            .subjects(subjects)
            .storageType(StorageType.Memory)
            .build();
        jsm.addStream(streamConfig);
    }
}

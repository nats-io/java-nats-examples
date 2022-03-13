package io.nats.jsmulti.examples;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.jsmulti.shared.Utils;

import java.io.IOException;
import java.util.List;

public class StreamUtils {

    public static void main(String[] args) throws Exception {
        setupStream("strm", "sub", "nats://localhost:4222");
    }

    public static void setupStream(String stream, String subject, String server) throws InterruptedException, IOException, JetStreamApiException {
        Options options = Utils.defaultOptions(server);
        try (Connection nc = Nats.connect(options)) {
            JetStreamManagement jsm = nc.jetStreamManagement();
            try {
                jsm.purgeStream(stream);
                List<String> cons = jsm.getConsumerNames(stream);
                for (String c : cons) {
                    jsm.deleteConsumer(stream, c);
                }
                System.out.println("PURGED: " + jsm.getStreamInfo(stream));
            }
            catch (JetStreamApiException j) {
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name(stream)
                    .subjects(subject)
                    .storageType(StorageType.Memory)
                    .build();
                System.out.println("CREATED: " + jsm.addStream(streamConfig));
            }
        }
    }
}

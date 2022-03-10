package io.nats.jsmulti.examples;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.jsmulti.shared.Utils;

import java.io.IOException;

import static io.nats.client.support.JsonUtils.getFormatted;

public class StreamUtils {

    public static void main(String[] args) throws Exception {
        setup("strm", "sub", "nats://localhost:4222");
    }

    public static void setup(String stream, String subject, String server) throws InterruptedException, IOException, JetStreamApiException {
        Options options = Utils.defaultOptions(server);
        try (Connection nc = Nats.connect(options)) {
            JetStreamManagement jsm = nc.jetStreamManagement();
            try {
                jsm.purgeStream(stream);
                System.out.println("PURGED\n" + getFormatted(jsm.getStreamInfo(stream)));
            }
            catch (JetStreamApiException j) {
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name(stream)
                    .subjects(subject)
                    .storageType(StorageType.Memory)
                    .build();
                System.out.println("CREATED\n" + getFormatted(jsm.addStream(streamConfig)));
            }
        }
    }
}

package io.nats.jsmulti.examples;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.jsmulti.internal.Context;
import io.nats.jsmulti.shared.DefaultOptionsFactory;

import java.util.List;

public class StreamUtils {

    public static void setupStream(String stream, String subject, String server) throws Exception {
        setupStream(stream, subject, DefaultOptionsFactory.getOptions(server));
    }

    public static void setupStream(String stream, String subject, Context context) throws Exception {
        setupStream(stream, subject, context.getOptions());
    }

    public static void setupStream(String stream, String subject, Options options) throws Exception {
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

package io.nats.jsmulti.examples;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.jsmulti.settings.Context;
import io.nats.jsmulti.shared.OptionsFactory;

import java.util.List;

public class StreamUtils {

    public static void setupStream(String stream, String subject, String server) throws Exception {
        setupStream(stream, subject, OptionsFactory.getOptions(server), OptionsFactory.getJetStreamOptions());
    }

    public static void setupStream(String stream, String subject, Context context) throws Exception {
        setupStream(stream, subject, context.getOptions(), context.getJetStreamOptions());
    }

    public static void setupStream(String stream, String subject, Options options, JetStreamOptions jso) throws Exception {
        try (Connection nc = Nats.connect(options)) {
            JetStreamManagement jsm = nc.jetStreamManagement(jso);
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

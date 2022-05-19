package io.nats.encoding;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

/*
Demonstrate various ways to encode
 */
public class EncodingVarious
{
    public static void main(String[] args) throws Exception
    {
        try (Connection nc = Nats.connect("nats://localhost")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Purge the stream. If it doesn't exist, an exception is thrown and we know to create it.
            try {
                jsm.purgeStream("stream");
            }
            catch (JetStreamApiException je) {
                if (je.getApiErrorCode() == 10059) {
                    StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name("stream")
                        .subjects("json", "gzip")
                        .storageType(StorageType.Memory)
                        .build();
                    jsm.addStream(streamConfig);
                }
                else {
                    throw je;
                }
            }

            JetStream js = nc.jetStream();
            json(js);
            gzip(js);
        }
    }

    private static void gzip(JetStream js) throws IOException, JetStreamApiException, InterruptedException {
        Pojo originalPojo = getPojo();
        String originalJson = JsonWriter.toJson(originalPojo);

        GZipper gz = new GZipper();
        gz.zip(originalJson.getBytes());
        byte[] originalZipped = gz.finish();
        js.publish("gzip", originalZipped);

        JetStreamSubscription sub = js.subscribe("gzip");
        Message subMessage = sub.nextMessage(Duration.ofSeconds(1));

        byte[] subUnzipped = GZipper.unzip(subMessage.getData());
        Pojo subPojo = JsonReader.read(subUnzipped, Pojo.class);

        sub.unsubscribe();

        System.out.println("\nGZIP\n----");
        System.out.println("ORIG JSON      : " + originalJson);
        System.out.println("ORIG LENGTH    : " + originalJson.length());
        System.out.println("ORIG GZIP LEN  : " + originalZipped.length);
        System.out.println("SUB RAW LEN    : " + subMessage.getData().length);
        System.out.println("SUB UNZIP LEN  : " + subUnzipped.length);
        System.out.println("SUB UNZIP JSON : " + JsonWriter.toJson(subPojo));
        System.out.println("ORIG EQ SUB    : " + subPojo.equals(originalPojo));
    }

    private static void json(JetStream js) throws IOException, JetStreamApiException, InterruptedException {
        Pojo originalPojo = getPojo();
        String originalJson = JsonWriter.toJson(originalPojo);

        js.publish("json", JsonWriter.toJsonBytes(originalPojo));

        JetStreamSubscription sub = js.subscribe("json");
        Message m = sub.nextMessage(Duration.ofSeconds(1));

        Pojo subPojo = JsonReader.read(m.getData(), Pojo.class);
        sub.unsubscribe();

        System.out.println("\nJSON\n----");
        System.out.println("ORIG JSON    : " + originalJson);
        System.out.println("SUB RAW DATA : " + new String(m.getData()));
        System.out.println("ORIG EQ SUB  : " + subPojo.equals(originalPojo));
    }

    private static Pojo getPojo() {
        Pojo ppub = new Pojo();
        ppub.s = "a bb  ccc   dddd    eeeee     ffffff      ggggggg       hhhhhhhh        iiiiiiiii         jjjjjjjjjj";
        ppub.l = 42L;
        ppub.b = true;
        ppub.strings = new ArrayList<>();
        ppub.strings.add("aaa");
        ppub.strings.add("bbb");
        ppub.ints = new Integer[2];
        ppub.ints[0] = 73;
        ppub.ints[1] = 99;
        return ppub;
    }
}

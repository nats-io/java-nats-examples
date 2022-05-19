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

    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost")) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration and create the stream
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("stream")
                    .subjects("json", "gzip")
                    .storageType(StorageType.Memory)
                    .build();
            jsm.addStream(streamConfig);
            jsm.purgeStream("stream");
            JetStream js = nc.jetStream();
            json(js);
            gzip(js);
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void gzip(JetStream js) throws IOException, JetStreamApiException, InterruptedException {
        System.out.println("\nGZIP");
        Pojo ppub = getPojo();
        String json = JsonWriter.toJson(ppub);
        System.out.println("JSON    : " + json);
        System.out.println("LENGTH  : " + json.length());
        GZipper gz = new GZipper();
        gz.zip(json.getBytes());
        byte[] zipped = gz.finish();
        System.out.println("ZIP LEN : " + zipped.length);
        js.publish("gzip", zipped);

        JetStreamSubscription sub = js.subscribe("gzip");
        Message m = sub.nextMessage(Duration.ofSeconds(1));
        System.out.println("RAW LEN : " + m.getData().length);

        byte[] unzipped = GZipper.unzip(m.getData());
        System.out.println("UN LEN  : " + unzipped.length);
        Pojo psub = JsonReader.read(unzipped, Pojo.class);
        System.out.println("UN JSON : " + JsonWriter.toJson(psub));
        System.out.println("EQUAL   : " + psub.equals(ppub));

        sub.unsubscribe();
    }

    private static void json(JetStream js) throws IOException, JetStreamApiException, InterruptedException {
        System.out.println("\nJSON");
        Pojo ppub = getPojo();
        System.out.println("IN    : " + JsonWriter.toJson(ppub));

        js.publish("json", JsonWriter.toJsonBytes(ppub));

        JetStreamSubscription sub = js.subscribe("json");
        Message m = sub.nextMessage(Duration.ofSeconds(1));
        System.out.println("RAW   : " + new String(m.getData()));

        Pojo psub = JsonReader.read(m.getData(), Pojo.class);
        System.out.println("EQUAL : " + psub.equals(ppub));

        sub.unsubscribe();
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

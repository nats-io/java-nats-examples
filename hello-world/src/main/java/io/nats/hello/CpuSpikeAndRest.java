package io.nats.hello;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;

public class CpuSpikeAndRest
{
    static final int PUB = 50000;
    static final int REST = 30000;
    static final String STREAM = "stream";
    static final String SUBJECT = "subject";
    static final byte[] DATA = new byte[128];

    public static void main( String[] args )
    {
        int pubCount = 0;
        try (Connection nc = Nats.connect("nats://localhost")) {
            createStream(nc);

            JetStream js = nc.jetStream();

            while (true) {
                System.out.print("PUB ");
                for (int x = 0; x < PUB; x++) {
                    pubCount++;
                    js.publish(SUBJECT, DATA);
                    if (x % 5000 == 0) {
                        System.out.print(".");
                    }
                }
                System.out.println(" " + pubCount);
                sleep(REST);
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void createStream(Connection nc) throws IOException {
        JetStreamManagement jsm = nc.jetStreamManagement();

        // Build the configuration and create the stream
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(STREAM)
                .subjects(SUBJECT)
                .storageType(StorageType.Memory)
                .build();

        try {
            jsm.addStream(streamConfig);
        } catch (JetStreamApiException e) {
            // ignore for create, means it existed
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

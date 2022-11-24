package io.nats.ft;

import io.nats.client.*;
import io.nats.client.impl.NatsMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Uploader {
    public static void upload(Connection conn, File f, String description, String contentType, boolean gzip) throws IOException, JetStreamApiException, NoSuchAlgorithmException {
        Debug.info("PUBLISHING " + f.getName() + ", " + description + ", " + contentType + (gzip ? ", gzip" : ""));
        GZipper gzipper = gzip ? new GZipper() : null;

        KeyValue kv = conn.keyValue(Constants.FILES_BUCKET);
        JetStreamOptions jso = JetStreamOptions.builder()
                .publishNoAck(true)
                .build();
        JetStream js = conn.jetStream(jso);

        long osLen = checkFile(f);
        Digester fileDigester = new Digester();
        Digester partDigester = new Digester();

        // initialize the FileMeta
        FileMeta fm = createFileMeta(f, osLen, description, contentType);

        // working with partSize number of bytes each time.
        byte[] buffer = new byte[Constants.PART_SIZE];

        long redLen = 0; // track total bytes read to make sure
        long partNumber = 0; // actual part numbers will start at 1

        try (FileInputStream in = new FileInputStream(f)) {
            // read the first chunk
            int red = in.read(buffer);
            while (red > 0) {
                // track total bytes just to make sure
                redLen += red;

                // initialize the PartMeta
                PartMeta pm = new PartMeta(fm, ++partNumber)
                        .start((partNumber-1) * Constants.PART_SIZE)
                        .length(red);

                // the payload is all bytes or red bytes depending
                byte[] payload = Arrays.copyOfRange(buffer, 0, red);

                // digest the actual bytes
                fileDigester.update(payload);
                pm.digest(partDigester.reset(payload).getDigestValue());

                // if asked to compress, update the payload
                if (gzip) {
                    payload = gzipper.clear().zip(payload).finish();
                    pm.encoded(Constants.GZIP, payload.length);
                }

                // publish the payload
                publishPart(js, fm, pm, payload);

                // read more if we got a full read last time, otherwise, that's the last of the bytes
                red = red == Constants.PART_SIZE ? in.read(buffer) : -1;
            }
        }

        if (redLen != f.length()) {
            // something went wrong
            throw new IOException("Mismatch bytes read versus expected.");
        }

        // update the FileMeta with the digest value
        fm.digest(fileDigester.getDigestValue());

        // publish the FileMeta
        publishFileMeta(kv, fm);
    }

    private static void publishPart(JetStream js, FileMeta fm, PartMeta pm, byte[] payload) throws IOException, JetStreamApiException {
        Debug.pubPart(fm, pm, payload, pm.getPartNumber() > 1);
        String messageSubject = Constants.PART_SUBJECT_PREFIX + fm.getId();
        js.publish(NatsMessage.builder()
                .subject(messageSubject)
                .data(payload)
                .headers(pm.toHeaders())
                .build());
    }

    private static void publishFileMeta(KeyValue kv, FileMeta fm) throws IOException, JetStreamApiException {
        Debug.info();
        Debug.pubFile(fm);
        kv.put(fm.getKvKey(), fm.toJson());
    }

    private static long checkFile(File f) throws IOException {
        if (!f.exists()) {
            throw new IOException("File does not exist: " + f.getAbsolutePath());
        }
        if (!f.canRead()) {
            throw new IOException("File is not readable: " + f.getAbsolutePath());
        }
        long len = f.length();
        if (len == 0) {
            throw new IOException("File is empty: " + f.getAbsolutePath());
        }
        return len;
    }

    private static FileMeta createFileMeta(File f, long length, String description, String contentType) throws NoSuchAlgorithmException {
        long parts = length / Constants.PART_SIZE;
        long lastPartSize = length - (parts * Constants.PART_SIZE);
        if (lastPartSize == 0) {
            lastPartSize = Constants.PART_SIZE;
        }
        else {
            parts++; // there is one part that isn't full size
        }

        return new FileMeta(f.getName(), contentType, length, f.lastModified())
                .description(description)
                .parts(parts)
                .partSize(Constants.PART_SIZE)
                .lastPartSize(lastPartSize);
    }
}

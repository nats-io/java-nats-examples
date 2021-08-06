package io.nats.ft;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamOptions;
import io.nats.client.impl.NatsMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Uploader
{
    public static void upload(Connection nc, int partSize, File f, String description, String contentType, String digestAlgorithm, boolean gzip) throws IOException, JetStreamApiException, NoSuchAlgorithmException {
        Zipper zipper = gzip ? new Zipper() : null;
        
        JetStreamOptions jso = JetStreamOptions.builder()
//                .publishNoAck(true)
                .build();
        JetStream js = nc.jetStream(jso);

        long osLen = checkFile(f);
        Digester fileDigester = new Digester(digestAlgorithm);
        Digester partDigester = new Digester(digestAlgorithm);

        // initialize the FileMeta, we need it's id for each part
        FileMeta fm = createFileMeta(f, partSize, osLen, description, contentType, digestAlgorithm);

        // working with partSize number of bytes each time.
        byte[] buffer = new byte[partSize];

        long redLen = 0; // track total bytes read to make sure
        long partNumber = 0; // actual part numbers will start at 1

        try (FileInputStream in = new FileInputStream(f)) {
            // read the first chunk
            int red = in.read(buffer);
            while (red > 0) {
                // track total bytes just to make sure
                redLen += red;

                PartMeta pm = new PartMeta(fm, ++partNumber)
                        .start((partNumber-1) * partSize)
                        .length(red)
                        .digestAlgorithm(digestAlgorithm);

                // the payload is all bytes or red bytes depending
                byte[] payload = red == partSize ? buffer : Arrays.copyOfRange(buffer, 0, red);

                // digest the actual bytes
                fileDigester.update(payload);
                pm.digestValue(partDigester.reset().update(payload).getDigestValue());

                // if asked to compress, update the payload
                if (gzip) {
                    payload = zipper.zip(payload);
                    pm.encodedLength(payload.length).contentEncoding(Constants.GZIP);
                }

                // publish the payload
                publishPart(js, fm, partNumber, pm, payload);

                // read more if we got a full read last time, otherwise, that's the last of the bytes
                red = red == partSize ? in.read(buffer) : -1;
            }
        }

        if (redLen != f.length()) {
            // something went wrong
            throw new IOException("Mismatch bytes read versus expected.");
        }

        // update the FileMeta with the digest value
        fm.digestValue(fileDigester.getDigestValue());

        // publish the FileMeta
        publishFileMeta(js, fm);
    }

    private static void publishPart(JetStream js, FileMeta fm, long partNumber, PartMeta pm, byte[] payload) throws IOException, JetStreamApiException {
        Debug.pubPart(fm, pm, payload);
        String messageSubject = Constants.PART_SUBJECT_PREFIX + fm.getId() + "." + partNumber;
        js.publish(NatsMessage.builder()
                .subject(messageSubject)
                .data(payload)
                .headers(pm.toHeaders())
                .build());
    }

    private static void publishFileMeta(JetStream js, FileMeta fm) throws IOException, JetStreamApiException {
        Debug.pubFile(fm);
        String messageSubject = Constants.FILE_NAME_SUBJECT_PREFIX + fm.getId();
        js.publish(NatsMessage.builder()
                .subject(messageSubject)
                .data(fm.toJson())
                .build());
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

    private static FileMeta createFileMeta(File f, int partSize, long length, String description, String contentType, String digestAlgorithm) throws NoSuchAlgorithmException {
        long parts = length / partSize;
        long lastPartSize = length - (parts * partSize);
        if (lastPartSize == 0) {
            lastPartSize = partSize;
        }
        else {
            parts++; // there is one part that isn't full size
        }

        return new FileMeta(f.getName(), contentType, length, f.lastModified())
                .description(description)
                .parts(parts)
                .partSize(partSize)
                .lastPartSize(lastPartSize)
                .digestAlgorithm(digestAlgorithm);
    }
}

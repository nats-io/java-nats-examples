package io.nats.ft;

import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.DeliverPolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.nats.ft.Constants.*;

public class Downloader
{
    public static void download(Connection nc, FileMeta fm, File outputDir) throws IOException, InterruptedException, JetStreamApiException, NoSuchAlgorithmException {
        JetStream js = nc.jetStream();
        JetStreamManagement jsm = nc.jetStreamManagement();

        Digester fileDigester = new Digester(fm.getDigestAlgorithm());
        Digester partDigester = new Digester(fm.getDigestAlgorithm());

        JetStreamSubscription sub = makeSub(js, fm, 1);
        printConsumerDebug(jsm);

        int fails = 10;
        long totalBytes = 0;
        long totalParts = 0;

        try (FileOutputStream out = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + fm.getName())) {
            long expecting = 1;
            long expectingAdjustment = 0;
            Message m = sub.nextMessage(Duration.ofSeconds(1));
            while (m != null) {
                // on big files with many messages, the server is going to get ahead of the client
                // and will send flow control messages. Check for and handle them.
                // If it's not flow control then it's a data message.
                if (checkFlowControl(nc, m)) {

                    // reconstitute the PartMeta from the headers
                    PartMeta pm = new PartMeta(m.getHeaders());

                    // do some checks to make sure we have the correct part

                    // ematch - consumer sequence should match the sequence we are expecting
                    boolean ematch = m.metaData().consumerSequence() == expecting;

                    // pmatch - consumer sequence + adjustment should match the part number we are expecting
                    boolean pmatch = m.metaData().consumerSequence() + expectingAdjustment == pm.getPartNumber();

                    boolean error = !ematch || !pmatch;

                    // dmatch - the digest from the PartMeta should match the digest of the payload (after unzipping if applicable)
                    //          (only calculated if there is no error)
                    boolean dmatch = false;

                    byte[] partBytes = null;
                    if (!error) {
                        // the part bytes are the data in the message
                        partBytes = m.getData();

                        // if it was zipped, unzip it
                        if (GZIP.equals(pm.getContentEncoding())) {
                            partBytes = Zipper.unzip(partBytes);
                        }

                        // figure it's digest and see if it matches
                        partDigester.reset().update(partBytes);
                        dmatch = pm.getDigestValue().equals(partDigester.getDigestValue());

                        error = !dmatch;
                    }

                    // debug printing here
                    if (expecting == 1 || error) {
                        printPartDebug(expecting, expectingAdjustment, m, pm, ematch, pmatch, dmatch);
                    }

                    // if there is an error, terminate the subscription
                    // and start a new one. Up to a limit of fails.
                    if (error) {
                        // terminate the message instead of acking and unsubscribe
                        m.term();
                        sub.unsubscribe();
                        flush(nc);

                        // don't fail indefinitely
                        if (--fails == 0) {
                            throw new RuntimeException("Too Many Mismatches");
                        }

                        // make a new subscription
                        sub = makeSub(js, fm, expecting);
                        flush(nc);
                        printConsumerDebug(jsm);

                        // reset counters
                        expectingAdjustment = expecting - 1;
                        expecting = 1;

                    } else {
                        // track the bytes / parts/ what is being expected
                        totalBytes += partBytes.length;
                        totalParts++;
                        expecting++;

                        // update the full file digest
                        fileDigester.update(partBytes);

                        // write the bytes to the output file
                        out.write(partBytes);

                        // ack the message since we are done processing it
                        m.ack();
                    }
                }

                // read until the subject is complete
                m = sub.nextMessage(Duration.ofSeconds(1));
            }
            out.flush();
        }
        printFileDebug(fm, fileDigester, totalBytes, totalParts);
    }

    private static boolean checkFlowControl(Connection nc, Message m) {
        if (m.isStatusMessage() && m.getStatus().isFlowControl()) {
            nc.publish(m.getReplyTo(), null);
            return false;
        }
        return true;
    }

    private static void flush(Connection nc) throws InterruptedException {
        try {
            nc.flush(Duration.ofSeconds(1));
        } catch (TimeoutException e) {
            // e.printStackTrace();
        }
    }

    private static void printFileDebug(FileMeta fm, Digester fileDigester, long totalBytes, long totalParts) {
        System.out.println("File: Di-" + fm.getDigestValue().equals(fileDigester.getDigestValue()) + " " + totalParts + " " + totalBytes + "\n    " + fm);
    }

    private static void printConsumerDebug(JetStreamManagement jsm) throws IOException, JetStreamApiException {
        List<ConsumerInfo> cis = jsm.getConsumers(PART_STREAM_NAME);
        System.out.println("Consumer: " + cis.get(cis.size() - 1));
    }

    private static void printPartDebug(long expecting, long expectingAdjustment, Message m, PartMeta pm, boolean ematch, boolean pmatch, Boolean dmatch) {
        System.out.println("Part: E-" + ematch + " P-" + pmatch + " D-" + dmatch + "\n    " + pm +
                "\n    Expecting " + expecting + "/" + (expecting + expectingAdjustment) + " Got "+ m.metaData().consumerSequence());
//                    for (int x = 0; x < 20; x++) {
//                        System.out.printf("%02x ", payload[x]);
//                    }
//                    System.out.println();
    }

    private static JetStreamSubscription makeSub(JetStream js, FileMeta fm, long startSeq) throws IOException, JetStreamApiException {
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
                .ackPolicy(AckPolicy.None)
                .deliverPolicy(DeliverPolicy.ByStartSequence)
                .startSequence(startSeq)
                .flowControl(true)
                .build();
        System.out.println("\nSub: " + cc.toString());
        PushSubscribeOptions pso = PushSubscribeOptions.builder().configuration(cc).build();
        return js.subscribe(PART_SUBJECT_PREFIX + fm.getId() + ".*", pso);
    }
}

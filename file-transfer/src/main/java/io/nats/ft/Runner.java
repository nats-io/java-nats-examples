package io.nats.ft;

import io.nats.client.*;
import io.nats.client.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Random;

import static io.nats.ft.Constants.*;

@SuppressWarnings("SameParameterValue")
public class Runner
{
    static final int DEFAULT_PART_SIZE = 1024 * 8;

    static class Input {
        final int partSize;
        final boolean textNotBinary;
        final long size;
        final String inputName;
        final File inputFile;
        final String inputDesc;

        public Input(long size, boolean textNotBinary) {
            this(size, textNotBinary, DEFAULT_PART_SIZE);
        }

        public Input(long size, boolean textNotBinary, int partSize) {
            this.partSize = partSize;
            this.textNotBinary = textNotBinary;
            this.size = size;
            inputName = textNotBinary ? "text-" + size + ".txt" : "binary-" + size + ".dat";
            inputFile = new File(INPUT_PATH + inputName);
            inputDesc = "" + size + " bytes " + (textNotBinary ? "text file" : "binary file");
        }
    }

    public static void main(String[] args) throws Exception {
        Downloader.MAX_DOWN_FAILS = 20;
        Input[] inputs = new Input[] {
                new Input(1_000, true),           // 1K, text
                new Input(10_000, true),          // 10K, text
                new Input(100_000, true),         // 100K, text
                new Input(1_000_000, true),       // 1Mb, text
                new Input(10_000_000, true),      // 10Mb, text
                new Input(100_000_000, true),     // 100Mb, text
                new Input(1_000_000_000, true),   // 1Gb, text
                new Input(10_000_000_000L, true), // 10Gb, text
                new Input(1_000, false),           // 1K, binary
                new Input(10_000, false),          // 10K, binary
                new Input(100_000, false),         // 100K, binary
                new Input(1_000_000, false),       // 1Mb, binary
                new Input(10_000_000, false),      // 10Mb, binary
                new Input(100_000_000, false),     // 100Mb, binary
                new Input(1_000_000_000, false),   // 1Gb, binary
                new Input(10_000_000_000L, false), // 10Gb, binary
        };

//        generateFiles(sizes);

        try (Connection nc = Nats.connect(SERVER)) {
//            setupStream(nc);
//            upload(nc, inputs);
//            download(nc, inputs);
//            listFilesAkaBucketKeys(nc);
//            listParts(nc, inputs);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void upload(Connection nc, Input[] inputs) throws JetStreamApiException, NoSuchAlgorithmException, IOException {
        for (Input input : inputs) {
            Uploader.upload(nc, input.partSize, input.inputFile, input.inputDesc,
                    input.textNotBinary ? TEXT_PLAIN : APPLICATION_OCTET_STREAM, // contentType
                    DIGEST_ALGORITHM, // digest algorithm
                    input.textNotBinary // gzip text files. binary files in my test actually gzip larger!
            );
        }
    }

    private static void download(Connection nc, Input[] inputs) throws JetStreamApiException, InterruptedException, IOException, NoSuchAlgorithmException {
        clearOutput();
        for (Input input : inputs) {
            Downloader.download(nc, getFileMeta(nc, input), new File(OUTPUT_DIR));
        }
    }

    private static void listParts(Connection nc, Input[] inputs) throws JetStreamApiException, InterruptedException, IOException {
        for (Input main : inputs) {
            FileMeta fm = getFileMeta(nc, main);
            Debug.write("Looked Up File: " + fm);
            JetStream js = nc.jetStream();
            PushSubscribeOptions pso = PushSubscribeOptions.builder()
                    .configuration(
                            ConsumerConfiguration.builder()
                                    .ackPolicy(AckPolicy.None)
                                    .build())
                    .build();
            JetStreamSubscription sub = js.subscribe(PART_SUBJECT_PREFIX + fm.getId(), pso);
            Debug.consumer(sub);
            Message msg = sub.nextMessage(Duration.ofSeconds(1));
            while (msg != null) {
                Debug.write(msg.getSubject());
                Debug.write(Debug.INDENT + new PartMeta(msg.getHeaders()));
                Debug.write(Debug.INDENT + msg.metaData());
                Debug.payloadBytes(Debug.INDENT + "Listed Data: ", true, msg.getData());
                msg = sub.nextMessage(Duration.ofSeconds(1));
            }
        }
    }

    private static FileMeta getFileMeta(Connection nc, Input main) throws IOException, JetStreamApiException {
        return new FileMeta(nc.keyValue(FILES_BUCKET).getValue(main.inputName));
    }

    private static void listFilesAkaBucketKeys(Connection nc) throws IOException, JetStreamApiException, InterruptedException {
        for (String k : nc.keyValueManagement().keys(FILES_BUCKET)) {
            System.out.println(k);
        }
    }

    public static StreamInfo getStreamInfo(JetStreamManagement jsm, String streamName) throws IOException, JetStreamApiException {
        try {
            return jsm.getStreamInfo(streamName);
        }
        catch (JetStreamApiException jsae) {
            if (jsae.getErrorCode() == 404) {
                return null;
            }
            throw jsae;
        }
    }

    private static void setupStream(Connection nc) throws IOException, JetStreamApiException {

        KeyValueManagement kvm = nc.keyValueManagement();
        JetStreamManagement jsm = nc.jetStreamManagement();

        // delete bucket if exists
        try {
            kvm.deleteBucket(FILES_BUCKET);
        }
        catch (JetStreamApiException e) {
            // 10059 means the bucket does not exist, which is fine. Other errors are bad.
            if (e.getApiErrorCode() != 10059) {
                throw e;
            }
        }

        // create the file kv bucket
        BucketConfiguration bc = BucketConfiguration.builder()
                .name(FILES_BUCKET)
                .maxHistory(5)
                .storageType(ST)
                .build();
        kvm.createBucket(bc);

        // delete stream if exists
        StreamInfo si = getStreamInfo(jsm, PART_STREAM_NAME);
        if (si != null) {
            jsm.deleteStream(PART_STREAM_NAME);
        }

        // build the parts stream
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(PART_STREAM_NAME)
                .subjects(PART_STREAM_SUBJECT)
                .storageType(ST)
                .build();

        // Create the stream
        jsm.addStream(streamConfig);
    }

    // ----------------------------------------------------------------------------------------------------
    // Data File Generation and not file transfer relevant
    // ----------------------------------------------------------------------------------------------------
    private static void generateFiles(long[] sizes) throws IOException {
        Random r = new Random();
        byte[] textBytes = Files.readAllBytes(new File(TEXT_SEED_FILE).toPath());

        try {
            for (long bytes : sizes) {
                generateText(bytes, textBytes);
                generateBinaryFile(bytes, r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateBinaryFile(long bytes, Random r) throws IOException {
        String filespec = INPUT_PATH + "binary-" + bytes + ".dat";
        try (FileOutputStream out = new FileOutputStream(filespec)) {
            long left = bytes;
            byte[] buf = new byte[1024];
            while (left >= 1024) {
                r.nextBytes(buf);
                out.write(buf);
                left -= 1024;
            }
            if (left > 0) {
                r.nextBytes(buf);
                out.write(buf, 0, (int)left);
            }
        }
    }

    private static void generateText(long bytes, byte[] textBytes) throws IOException {
        String filespec = INPUT_PATH + "text-" + bytes + ".txt";
        try (FileOutputStream out = new FileOutputStream(filespec)) {
            int tbPtr = -1024;
            long left = bytes;
            while (left >= 1024) {
                tbPtr += 1024;
                if (tbPtr + 1023 > textBytes.length) {
                    tbPtr = 0;
                }
                out.write(textBytes, tbPtr, 1024);
                left -= 1024;
            }
            if (left > 0) {
                out.write(textBytes, 0, (int)left);
            }
        }
    }

    @SuppressWarnings({"DuplicatedCode", "ResultOfMethodCallIgnored"})
    private static void clearOutput() {
        File[] files = new File(OUTPUT_DIR).listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }
}

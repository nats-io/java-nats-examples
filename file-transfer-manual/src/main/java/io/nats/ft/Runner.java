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
import static io.nats.ft.FileMeta.getKvKey;

public class Runner
{
    public static void main(String[] args) throws Exception {

        Input[] inputs = new Input[] {
//                new Input(1_000, true),           // 1K, text
//                new Input(10_000, true),          // 10K, text
//                new Input(100_000, true),         // 100K, text
//                new Input(1_000_000, true),       // 1Mb, text
//                new Input(10_000_000, true),      // 10Mb, text
//                new Input(100_000_000, true),     // 100Mb, text
//                new Input(1_000_000_000, true),   // 1Gb, text
//                new Input(10_000_000_000L, true), // 10Gb, text
//                new Input(1_000, false),           // 1K, binary
//                new Input(10_000, false),          // 10K, binary
//                new Input(100_000, false),         // 100K, binary
//                new Input(1_000_000, false),       // 1Mb, binary
//                new Input(10_000_000, false),      // 10Mb, binary
//                new Input(100_000_000, false),     // 100Mb, binary
//                new Input(1_000_000_000, false),   // 1Gb, binary
//                new Input(10_000_000_000L, false), // 10Gb, binary
        };

//        generateFiles(inputs);

        try (Connection conn = Nats.connect(SERVER)) {
//            setup(conn);
//            upload(conn, inputs);
//            download(conn, inputs);
//            listParts(conn, inputs);
//            listFilesAkaBucketKeys(conn);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void upload(Connection conn, Input[] inputs) throws JetStreamApiException, NoSuchAlgorithmException, IOException {
        for (Input input : inputs) {
            Uploader.upload(conn, input.inputFile, input.inputDesc,
                    input.textNotBinary ? TEXT_PLAIN : APPLICATION_OCTET_STREAM, // contentType
                    input.textNotBinary // gzip text files. binary files in my test actually gzip larger!
            );
        }
    }

    private static void download(Connection conn, Input[] inputs) throws JetStreamApiException, InterruptedException, IOException, NoSuchAlgorithmException {
        clearOutput();
        for (Input input : inputs) {
            Downloader.download(conn, getFileMeta(conn, input), new File(OUTPUT_DIR));
        }
    }

    private static void listParts(Connection conn, Input[] inputs) throws JetStreamApiException, InterruptedException, IOException {
        for (Input main : inputs) {
            FileMeta fm = getFileMeta(conn, main);
            Debug.write("Looked Up File: " + fm);
            JetStream js = conn.jetStream();
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

    private static FileMeta getFileMeta(Connection conn, Input main) throws IOException, JetStreamApiException {
        return new FileMeta(conn.keyValue(FILES_BUCKET).get(getKvKey(main.inputName)).getValueAsString());
    }

    private static void listFilesAkaBucketKeys(Connection conn) throws IOException, JetStreamApiException, InterruptedException {
        for (String k : conn.keyValue(FILES_BUCKET).keys()) {
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

    private static void setup(Connection conn) throws IOException, JetStreamApiException {

        KeyValueManagement kvm = conn.keyValueManagement();
        JetStreamManagement jsm = conn.jetStreamManagement();

        // delete bucket if exists
        try {
            kvm.delete(FILES_BUCKET);
        }
        catch (JetStreamApiException e) {
            // 10059 means the bucket does not exist, which is fine. Other errors are bad.
            if (e.getApiErrorCode() != 10059) {
                throw e;
            }
        }

        // create the file kv bucket
        KeyValueConfiguration kvc = KeyValueConfiguration.builder()
                .name(FILES_BUCKET)
                .maxHistoryPerKey(5)
                .storageType(ST)
                .build();
        kvm.create(kvc);

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
    private static void generateFiles(Input[] inputs) throws IOException {
        Random r = new Random();
        byte[] textBytes = Files.readAllBytes(new File(TEXT_SEED_FILE).toPath());

        try {
            for (Input input : inputs) {
                if (input.textNotBinary) {
                    generateText(input.size, textBytes);
                }
                else {
                    generateBinaryFile(input.size, r);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateBinaryFile(long bytes, Random r) throws IOException {
        String filespec = DATA_INPUT_DIR + "binary-" + bytes + ".dat";
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
        String filespec = DATA_INPUT_DIR + "text-" + bytes + ".txt";
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

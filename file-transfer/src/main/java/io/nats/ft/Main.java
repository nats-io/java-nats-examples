package io.nats.ft;

import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StreamConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.nats.ft.Constants.*;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class Main
{
    final int partSize;
    final boolean text;
    final long size;
    final String inputName;
    final File inputFile;
    final String inputDesc;

    public Main(int partSize, long size, boolean text) {
        this.partSize = partSize;
        this.text = text;
        this.size = size;
        inputName = text ? "text-" + size + ".txt" : "binary-" + size + ".dat";
        inputFile = new File(INPUT_PATH + inputName);
        inputDesc = "" + size + " bytes " + (text ? "text file" : "binary file");
    }

    public static void main(String[] args) throws Exception {
        Main m = new Main(1024 * 8, 100_000, true);

        try (Connection nc = Nats.connect(Constants.SERVER)) {
            setupStream(nc);
            up(nc, m);
            clearOutput(); down(nc, m);
//            list(nc, m);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void up(Connection nc, Main m) throws JetStreamApiException, NoSuchAlgorithmException, IOException {
        Uploader.upload(nc, m.partSize, m.inputFile, m.inputDesc,
                m.text ? TEXT_PLAIN : APPLICATION_OCTET_STREAM, // contentType
                DIGEST_ALGORITHM, // digest algorithm
                false
                // m.text // gzip text files. binary files in my test actually gzip larger!
        );
    }

    private static void down(Connection nc, Main m) throws JetStreamApiException, InterruptedException, IOException, NoSuchAlgorithmException {
        List<FileMeta> list = listFiles(nc);
        Downloader.download(nc, getFile(list, m.inputName), new File(OUTPUT_DIR));
    }

    private static void list(Connection nc, Main m) throws JetStreamApiException, InterruptedException, IOException {
        List<FileMeta> list = listFiles(nc);
        FileMeta fm = getFile(list, m.inputName);
        listParts(nc, fm.getId());
    }

    private static void listParts(Connection nc, String uuid) throws IOException, JetStreamApiException, InterruptedException {
        JetStream js = nc.jetStream();
        PushSubscribeOptions pso = PushSubscribeOptions.builder()
                .configuration(
                        ConsumerConfiguration.builder()
                                .ackPolicy(AckPolicy.None)
                                .build())
                .build();
        JetStreamSubscription sub = js.subscribe(PART_SUBJECT_PREFIX + uuid + ".*", pso);
        Message m = sub.nextMessage(Duration.ofSeconds(1));
        while (m != null) {
            System.out.println(m.getSubject() + "\n    " + new PartMeta(m.getHeaders()) + "\n    " + m.metaData());
            m = sub.nextMessage(Duration.ofSeconds(1));
        }
    }

    private static FileMeta getFile(List<FileMeta> list, String name) {
        FileMeta found = null;
        for (FileMeta fm : list) {
            if (fm.getName().equals(name)) {
                found = fm;
            }
        }
        return found;
    }

    private static List<FileMeta> listFiles(Connection nc) throws IOException, JetStreamApiException, InterruptedException {
        List<FileMeta> list = new ArrayList<>();
        JetStream js = nc.jetStream();
        PushSubscribeOptions pso = PushSubscribeOptions.builder()
                .configuration(ConsumerConfiguration.builder().ackPolicy(AckPolicy.None).build())
                .build();
        JetStreamSubscription sub = js.subscribe(FILE_NAME_STREAM_SUBJECT, pso);
        Message m = sub.nextMessage(Duration.ofSeconds(1));
        while (m != null) {
            FileMeta fm = new FileMeta(new String(m.getData(), US_ASCII));
            list.add(fm);
            m = sub.nextMessage(Duration.ofSeconds(1));
        }
        return list;
    }

    private static void setupStream(Connection nc) throws IOException, JetStreamApiException {

        JetStreamManagement jsm = nc.jetStreamManagement();

        // build the parts stream
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(Constants.PART_STREAM_NAME)
                .subjects(Constants.PART_STREAM_SUBJECT)
                .storageType(ST)
                .build();

        // Create the stream
        jsm.addStream(streamConfig);

        // build the file kv stream
        StreamConfiguration scBucket = StreamConfiguration.builder()
                .name(Constants.FILE_NAME_STREAM)
                .subjects(Constants.FILE_NAME_STREAM_SUBJECT)
                .storageType(ST)
                .build();

        // Create the stream
        jsm.addStream(scBucket);
    }

    // ----------------------------------------------------------------------------------------------------
    // Data File Generation and not file transfer relevant
    // ----------------------------------------------------------------------------------------------------
    private static void generateFiles() throws IOException {
        Random r = new Random();
        byte[] textBytes = Files.readAllBytes(new File(TEXT_SEED_FILE).toPath());

        try {
            for (long x = 1000; x <= 10_000_000_000L; x *= 10) {
                generateText(x, textBytes);
                generateBinaryFile(x, r);
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

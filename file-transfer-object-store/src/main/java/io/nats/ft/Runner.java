package io.nats.ft;

import io.nats.client.*;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import io.nats.client.api.ObjectStoreConfiguration;
import io.nats.client.impl.ErrorListenerLoggerImpl;
import io.nats.client.support.Digester;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static io.nats.ft.Constants.*;

public class Runner
{
    static DataFile[] RUN_DATA = new DataFile[] {
        TEXT_1K
        ,TEXT_10K
        ,TEXT_100K
        ,TEXT_1MB
        ,TEXT_10MB
        ,TEXT_100MB
//        ,TEXT_1GB
//        ,TEXT_5GB
//        ,TEXT_10GB
//        ,BINARY_1K
//        ,BINARY_10K
//        ,BINARY_100K
//        ,BINARY_1MB
//        ,BINARY_10MB
//        ,BINARY_100MB
//        ,BINARY_1GB
//        ,BINARY_5GB
//        ,BINARY_10GB
    };

    public static void main(String[] args) throws Exception {

        // Generate some data files. See Constants. Comment out when data exists.
        DataFile.generate(RUN_DATA);

        try (Connection conn = Nats.connect(getOptions())) {
            // Setup deletes an existing store, makes a new one.
            // Comment out if the store is set up, maye to see what upload over an object does
            setupStore(conn);

            // upload objects
            upload(conn, RUN_DATA);

            // print info for objects in the store
            printInfos(conn);

            // download objects
            download(conn, RUN_DATA);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupStore(Connection conn) throws IOException, JetStreamApiException {
        ObjectStoreManagement osm = conn.objectStoreManagement();

        // delete bucket if exists
        try {
            osm.delete(STORE_BUCKET_NAME);
        }
        catch (JetStreamApiException e) {
            // 10059 means the bucket does not exist, which is fine. Other errors are bad.
            if (e.getApiErrorCode() != 10059) {
                throw e;
            }
        }

        // create the file kv bucket
        ObjectStoreConfiguration kvc = ObjectStoreConfiguration.builder(STORE_BUCKET_NAME)
            .storageType(STORAGE_TYPE)
            .build();
        osm.create(kvc);
    }

    private static void upload(Connection conn, DataFile[] dataFiles) throws IOException, JetStreamApiException, NoSuchAlgorithmException {
        ObjectStore os = conn.objectStore(STORE_BUCKET_NAME);
        for (DataFile df : dataFiles) {
            InputStream in = Files.newInputStream(df.inputFile.toPath());
            ObjectMeta meta = ObjectMeta.builder(df.name)
                .description(df.description)
                .chunkSize(CHUNK_SIZE)
                .build();
            os.put(meta, in);
            printObjectInfo("Upload: ", os.getInfo(df.name));
            ObjectInfo oi = os.getInfo(df.name);
            String dIn = getDigestEntry(df.inputFile);
            checkDigests("  | Input vs ObjectInfo", dIn, oi.getDigest());
        }
    }

    private static void printInfos(Connection conn) throws JetStreamApiException, InterruptedException, IOException, NoSuchAlgorithmException {
        ObjectStore os = conn.objectStore(STORE_BUCKET_NAME);
        List<ObjectInfo> list = os.getList();
        list.forEach(oi -> printObjectInfo("Info: ", oi));
    }

    private static void printObjectInfo(String label, ObjectInfo oi) {
        ObjectMeta meta = oi.getObjectMeta();
        System.out.println(label + meta.getObjectName()
            + " [" + meta.getDescription() + "]"
            + "\n  | nuid='" + oi.getNuid() + '\''
            + " size=" + oi.getSize()
            + " chunks=" + oi.getChunks()
            + " digest='" + oi.getDigest() + '\''
            + " modified=" + oi.getModified());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void download(Connection conn, DataFile[] dataFiles) throws JetStreamApiException, InterruptedException, IOException, NoSuchAlgorithmException {
        ObjectStore os = conn.objectStore(STORE_BUCKET_NAME);
        for (DataFile df : dataFiles) {
            System.out.println("Downloading " + df.name);
            if (df.outputFile.exists()) {  // remove old file if it exists
                df.outputFile.delete();
            }
            try (OutputStream out = Files.newOutputStream(df.outputFile.toPath())) {
                os.get(df.name, out);
                out.flush();
            }
            String dOut = getDigestEntry(df.inputFile);
            ObjectInfo oi = os.getInfo(df.name);
            checkDigests("  | Download vs ObjectInfo", dOut, oi.getDigest());
        }
    }

    private static void checkDigests(String label, String d1, String d2) {
        if (d1.equals(d2)) {
            System.out.println(label + " digests match.");
        }
        else {
            System.out.println(label + " digests do not match: " + d1 + " vs " + d2);
        }
    }

    public static String getDigestEntry(File f) throws IOException, NoSuchAlgorithmException {
        byte[] buff = new byte[1024];
        Digester d = new Digester();
        try (FileInputStream in = new FileInputStream(f)) {
            int red = in.read(buff, 0, 1024);
            while (red != -1) {
                d.update(buff, 0, red);
                red = in.read(buff, 0, 1024);
            }
        }
        return d.getDigestEntry();
    }

    private static Options getOptions() {
        ErrorListener el = new ErrorListenerLoggerImpl() {
            @Override
            public void flowControlProcessed(Connection conn, JetStreamSubscription sub, String id, FlowControlSource source) {
                // do nothing. flow control happens during downloads, it's just an info, not a warning.
            }
        };

        return new Options.Builder()
            .server(SERVER)
            .errorListener(el)
            .build();
    }
}

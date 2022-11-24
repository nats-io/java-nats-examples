package io.nats.ft;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

public class FileZipper implements Runnable {
    private final int bufferSize = 1024;
    private final PipedOutputStream pipedOut;
    private final GZIPOutputStream zipOut;
    private final FileInputStream fileIn;
    public final PipedInputStream pipedIn;
    public final CountDownLatch latch;
    public final AtomicReference<Exception> error;

    public FileZipper(File inputFile) throws IOException {
        fileIn = new FileInputStream(inputFile);
        latch = new CountDownLatch(1);
        error = new AtomicReference<>();
        pipedOut = new PipedOutputStream();
        pipedIn = new PipedInputStream(pipedOut); // connect the pipedIn before starting the Gzip
        zipOut = new GZIPOutputStream(pipedOut);
    }

    @Override
    public void run() {
        byte[] buff = new byte[bufferSize];
        int tot = 0;
        int red = 0;
        try {
            red = fileIn.read(buff);
            while (red != -1) {
                tot += red;
                System.out.println(red + "/" + tot);
                zipOut.write(buff, 0, red);
                red = fileIn.read(buff);
            }
            System.out.println("b4 flush");
            zipOut.flush();
            System.out.println("b4 finish");
            zipOut.finish();
            System.out.println("end");
        }
        catch (Exception e) {
            System.out.println(e);
            error.set(e);
            throw new RuntimeException(e);
        }
        finally {
            System.out.println("Done " + red + "/" + tot);
            latch.countDown();
            try { fileIn.close(); } catch (IOException ignore) {}
        }
    }

    public FileZipper zip(byte[] bytes) throws IOException {
        return zip(bytes, 0, bytes.length);
    }

    public FileZipper zip(byte[] bytes, int off, int len) throws IOException {
        zipOut.write(bytes, off, len);
        return this;
    }
}

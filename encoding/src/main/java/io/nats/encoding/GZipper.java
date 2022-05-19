package io.nats.encoding;

import io.nats.client.support.ByteArrayBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipper {
    private final ByteArrayBuilder bab;
    private final OutputStream out;
    private GZIPOutputStream zipOut;

    public GZipper() throws IOException {
        bab = new ByteArrayBuilder();
        out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                bab.append(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                bab.append(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                bab.append(b, off, len);
            }
        };
        clear();
    }

    public GZipper clear() throws IOException {
        bab.clear();
        zipOut = new GZIPOutputStream(out);
        return this;
    }

    public byte[] finish() throws IOException {
        zipOut.flush();
        zipOut.finish();
        return bab.toByteArray();
    }

    public GZipper zip(byte[] bytes) throws IOException {
        return zip(bytes, 0, bytes.length);
    }

    public GZipper zip(byte[] bytes, int off, int len) throws IOException {
        zipOut.write(bytes, off, len);
        return this;
    }

    public static byte[] unzip(byte[] bytes) throws IOException {
        return unzip(bytes, 1024, bytes.length * 2);
    }

    public static byte[] unzip(byte[] bytes, int projectedUnzippedSize) throws IOException {
        return unzip(bytes, 1024, projectedUnzippedSize);
    }

    public static byte[] unzip(byte[] bytes, int readBlockSize, int projectedUnzippedSize) throws IOException {
        ByteArrayBuilder bab = new ByteArrayBuilder(projectedUnzippedSize);
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes));
        byte[] buffer = new byte[readBlockSize];
        int red = in.read(buffer, 0, readBlockSize);
        while (red > 0) {
            // track what we got from the unzip
            bab.append(buffer, 0, red);

            // read more if we got a full read last time, otherwise, that's the last of the bytes
            red = red == readBlockSize ? in.read(buffer, 0, readBlockSize) : -1;
        }
        return bab.toByteArray();
    }
}

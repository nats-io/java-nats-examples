package io.nats.ft;

import io.nats.client.support.ByteArrayBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Zipper {
    private final ByteArrayBuilder bab;
    private final OutputStream out;

    public Zipper() {
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
    }

    public byte[] zip(byte[] bytes) throws IOException {
        return zip(bytes, 0, bytes.length);
    }

    public byte[] zip(byte[] bytes, int off, int len) throws IOException {
        bab.clear();
        GZIPOutputStream zip = new GZIPOutputStream(out);
        zip.write(bytes, off, len);
        zip.flush();
        zip.finish();
        return bab.toByteArray();
    }

    public static byte[] unzip(byte[] bytes) throws IOException {
        ByteArrayBuilder bab = new ByteArrayBuilder(bytes.length * 2);
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes));
        byte[] buffer = new byte[1024];
        int red = in.read(buffer, 0, 1024);
        while (red > 0) {
            // track what we got from the unzip
            bab.append(buffer, 0, red);

            // read more if we got a full read last time, otherwise, that's the last of the bytes
            red = red == 1024 ? in.read(buffer, 0, 1024) : -1;
        }
        return bab.toByteArray();
    }
}

package io.nats.ft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import static io.nats.ft.Constants.*;

public class DataFile
{
    final long size;
    final boolean isTextIsNotBinary;
    final String name;
    final String description;
    final File inputFile;
    final File outputFile;

    public DataFile(long size, boolean isTextIsNotBinary) {
        this.size = size;
        this.isTextIsNotBinary = isTextIsNotBinary;
        name = isTextIsNotBinary ? "text-" + size + ".txt" : "binary-" + size + ".dat";
        description = "" + size + " bytes " + (isTextIsNotBinary ? "text file" : "binary file");
        inputFile = new File(INPUT_DIR + name);
        outputFile = new File(OUTPUT_DIR + name);
    }

    public static void generate(DataFile[] dataFiles) throws IOException {
        Random r = new Random();
        byte[] textBytes = Files.readAllBytes(new File(TEXT_SEED_FILE).toPath());

        try {
            for (DataFile dataFile : dataFiles) {
                if (dataFile.isTextIsNotBinary) {
                    generateText(dataFile, textBytes);
                }
                else {
                    generateBinaryFile(dataFile, r);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateBinaryFile(DataFile df, Random r) throws IOException {
        try (FileOutputStream out = new FileOutputStream(df.inputFile)) {
            long left = df.size;
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

    public static void generateText(DataFile df, byte[] textBytes) throws IOException {
        try (FileOutputStream out = new FileOutputStream(df.inputFile)) {
            int tbPtr = -1024;
            long left = df.size;
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
}

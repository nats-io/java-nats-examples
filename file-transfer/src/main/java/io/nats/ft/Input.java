package io.nats.ft;

import java.io.File;

public class Input
{
    final int partSize;
    final boolean textNotBinary;
    final long size;
    final String inputName;
    final File inputFile;
    final String inputDesc;

    public Input(long size, boolean textNotBinary) {
        this.partSize = Constants.PART_SIZE;
        this.textNotBinary = textNotBinary;
        this.size = size;
        inputName = textNotBinary ? "text-" + size + ".txt" : "binary-" + size + ".dat";
        inputFile = new File(Constants.DATA_INPUT_DIR + inputName);
        inputDesc = "" + size + " bytes " + (textNotBinary ? "text file" : "binary file");
    }
}

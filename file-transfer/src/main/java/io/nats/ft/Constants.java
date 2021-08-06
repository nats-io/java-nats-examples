package io.nats.ft;

import io.nats.client.api.StorageType;

public interface Constants
{
    // ----------------------------------------------------------------------------------------------------
    // App Specific
    // ----------------------------------------------------------------------------------------------------

    String SERVER = "nats://localhost:4222";
    StorageType ST = StorageType.Memory;

    String META_HEADER_PREFIX = "io.nats.file.";

    String PART_STREAM_NAME = "file-transfer-parts";
    String PART_SUBJECT_PREFIX = "part.";
    String PART_STREAM_SUBJECT = PART_SUBJECT_PREFIX + ">";

    String FILE_NAME_BUCKET = "file-transfer-files";
    String FILE_NAME_SUBJECT_PREFIX = "$KV." + Constants.FILE_NAME_BUCKET + ".";
    String FILE_NAME_STREAM = "KV_" + Constants.FILE_NAME_BUCKET;
    String FILE_NAME_STREAM_SUBJECT = FILE_NAME_SUBJECT_PREFIX + "*";

    boolean GRANULAR_SUBJECT = false;

    String DIGEST_ALGORITHM = "SHA-256";

    String INPUT_PATH = "C:\\temp\\file-transfer\\in\\";
    String OUTPUT_DIR = "C:\\temp\\file-transfer\\out\\";
    String TEXT_SEED_FILE = "C:\\temp\\file-transfer\\text-seed.txt";
    String DEBUG_DIR = "C:\\temp\\file-transfer\\";

    // ----------------------------------------------------------------------------------------------------
    // General
    // ----------------------------------------------------------------------------------------------------

    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String TEXT_PLAIN = "text/plain";
    String GZIP = "gzip";
}

package io.nats.ft;

import io.nats.client.api.StorageType;

public interface Constants
{
    // ----------------------------------------------------------------------------------------------------
    // App Specific
    // ----------------------------------------------------------------------------------------------------

    String SERVER = "nats://localhost:4222";
    StorageType ST = StorageType.Memory;

    String PART_META_HEADER_PREFIX = "io.nats.part.";

    String FILES_BUCKET = "files";

    String PART_STREAM_NAME = "parts";
    String PART_SUBJECT_PREFIX = "part.";
    String PART_STREAM_SUBJECT = PART_SUBJECT_PREFIX + "*";

    String DIGEST_ALGORITHM = "SHA-256";

    int PART_SIZE = 1024 * 8;
    int MAX_DOWN_FAILS = 10;

    // make sure directories end with slash
    String DATA_INPUT_DIR = "C:\\temp\\file-transfer\\in\\";
    String OUTPUT_DIR = "C:\\temp\\file-transfer\\out\\";
    String DEBUG_OUTPUT_DIR = "C:\\temp\\file-transfer\\";

    String TEXT_SEED_FILE = "C:\\temp\\file-transfer\\text-seed.txt";

    // ----------------------------------------------------------------------------------------------------
    // General
    // ----------------------------------------------------------------------------------------------------

    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String TEXT_PLAIN = "text/plain";
    String GZIP = "gzip";
}

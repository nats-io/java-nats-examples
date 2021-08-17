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

    String PART_STREAM_NAME = "parts";
    String PART_SUBJECT_PREFIX = "part.";
    String PART_STREAM_SUBJECT = PART_SUBJECT_PREFIX + ">";

    String FILES_BUCKET = "files";

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

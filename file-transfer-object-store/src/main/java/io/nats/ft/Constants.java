package io.nats.ft;

import io.nats.client.api.StorageType;

public interface Constants
{
    String SERVER = "nats://localhost:4222";
    StorageType STORAGE_TYPE = StorageType.Memory;
    String STORE_BUCKET_NAME = "transfer";
    int CHUNK_SIZE = 8 * 1024;

    String INPUT_DIR = "C:\\temp\\file-transfer\\in\\";
    String OUTPUT_DIR = "C:\\temp\\file-transfer\\out\\";
    String TEXT_SEED_FILE = "C:\\temp\\file-transfer\\text-seed.txt";

    int SIZE_1K = 1024;
    int SIZE_10K = 10 * 1024;
    int SIZE_100K = 100 * 1024;
    int SIZE_1MB = 1024 * 1024;
    int SIZE_10MB = 10 * 1024 * 1024;
    int SIZE_100MB = 100 * 1024 * 1024;
    int SIZE_1GB = 1024 * 1024 * 1024;
    long SIZE_5GB = 5 * 1024 * 1024 * 1024L;
    long SIZE_10GB = 10 * 1024 * 1024 * 1024L;

    DataFile TEXT_1K = new DataFile(SIZE_1K, true);
    DataFile TEXT_10K = new DataFile(SIZE_10K, true);
    DataFile TEXT_100K = new DataFile(SIZE_100K, true);
    DataFile TEXT_1MB = new DataFile(SIZE_1MB, true);
    DataFile TEXT_10MB = new DataFile(SIZE_10MB, true);
    DataFile TEXT_100MB = new DataFile(SIZE_100MB, true);
    DataFile TEXT_1GB = new DataFile(SIZE_1GB, true);
    DataFile TEXT_5GB = new DataFile(SIZE_5GB, true);
    DataFile TEXT_10GB = new DataFile(SIZE_10GB, true);
    DataFile BINARY_1K = new DataFile(SIZE_1K, false);
    DataFile BINARY_10K = new DataFile(SIZE_10K, false);
    DataFile BINARY_100K = new DataFile(SIZE_100K, false);
    DataFile BINARY_1MB = new DataFile(SIZE_1MB, false);
    DataFile BINARY_10MB = new DataFile(SIZE_10MB, false);
    DataFile BINARY_100MB = new DataFile(SIZE_100MB, false);
    DataFile BINARY_1GB = new DataFile(SIZE_1GB, false);
    DataFile BINARY_5GB = new DataFile(SIZE_5GB, false);
    DataFile BINARY_10GB = new DataFile(SIZE_10GB, false);
}

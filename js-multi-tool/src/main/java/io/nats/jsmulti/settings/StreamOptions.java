package io.nats.jsmulti.settings;

import io.nats.client.api.StorageType;

public class StreamOptions {
    public static final StorageType DEFAULT_STORAGE_TYPE = StorageType.Memory;
    public static final int DEFAULT_REPLICAS = 1;
    public static final int DEFAULT_MAX_BYTES = -1;

    public StorageType storageType;
    public long maxBytes;
    public int replicas;

    public StreamOptions() {
        this(DEFAULT_STORAGE_TYPE, DEFAULT_MAX_BYTES, DEFAULT_REPLICAS);
    }

    public StreamOptions(StorageType storageType) {
        this(storageType, DEFAULT_MAX_BYTES, DEFAULT_REPLICAS);
    }

    public StreamOptions(long maxBytes) {
        this(DEFAULT_STORAGE_TYPE, maxBytes, DEFAULT_REPLICAS);
    }

    public StreamOptions(int replicas) {
        this(DEFAULT_STORAGE_TYPE, DEFAULT_MAX_BYTES, replicas);
    }

    public StreamOptions(StorageType storageType, long maxBytes) {
        this(storageType, maxBytes, DEFAULT_REPLICAS);
    }

    public StreamOptions(StorageType storageType, int replicas) {
        this(storageType, DEFAULT_MAX_BYTES, replicas);
    }

    public StreamOptions(StorageType storageType, long maxBytes, int replicas) {
        this.storageType = storageType;
        this.maxBytes = maxBytes;
        this.replicas = replicas;
    }

    public StreamOptions storageType(StorageType storageType) {
        this.storageType = storageType;
        return this;
    }

    public StreamOptions maxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
        return this;
    }

    public StreamOptions replicas(int replicas) {
        this.replicas = replicas;
        return this;
    }
}

package io.nats.ft;

import io.nats.client.support.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static io.nats.ft.Meta.Field.*;

public class FileMeta extends Meta
{
    private String name;
    private String description;
    private String contentType;
    private long parts;
    private long partSize;
    private long lastPartSize;
    private long fileDate;

    public FileMeta(String name, String contentType, long length, long fileDate) throws NoSuchAlgorithmException {
        id = new Digester("SHA-256")
                .update(name)
                .update(contentType)
                .update(length)
                .update(fileDate)
                .getDigestValue();
        this.name = name;
        this.contentType = contentType;
        this.length = length;
        this.fileDate = fileDate;
    }

    public FileMeta(byte[] raw) {
        this(new String(raw, StandardCharsets.UTF_8));
    }

    public FileMeta(String json) {
        id = JsonUtils.readString(json, ID.jsonRe);
        name = JsonUtils.readString(json, NAME.jsonRe);
        description = JsonUtils.readString(json, DESCRIPTION.jsonRe);
        contentType = JsonUtils.readString(json, CONTENT_TYPE.jsonRe);
        length = JsonUtils.readLong(json, LENGTH.jsonRe, 0);
        parts = JsonUtils.readLong(json, PARTS.jsonRe, 0);
        partSize = JsonUtils.readLong(json, PART_SIZE.jsonRe, 0);
        lastPartSize = JsonUtils.readLong(json, LAST_PART_SIZE.jsonRe, 0);
        fileDate = JsonUtils.readLong(json, FILE_DATE.jsonRe, 0);
        parseDigest(JsonUtils.readString(json, DIGEST.jsonRe));
    }

    public String toJson() {
        StringBuilder sb = JsonUtils.beginJson();
        JsonUtils.addField(sb, ID.jsonKey, id);
        JsonUtils.addField(sb, NAME.jsonKey, name);
        JsonUtils.addField(sb, DESCRIPTION.jsonKey, description);
        JsonUtils.addField(sb, CONTENT_TYPE.jsonKey, contentType);
        JsonUtils.addField(sb, LENGTH.jsonKey, length);
        JsonUtils.addField(sb, PARTS.jsonKey, parts);
        JsonUtils.addField(sb, PART_SIZE.jsonKey, partSize);
        JsonUtils.addField(sb, LAST_PART_SIZE.jsonKey, lastPartSize);
        JsonUtils.addField(sb, FILE_DATE.jsonKey, fileDate);
        if (digestAlgorithm != null && digestValue != null) {
            JsonUtils.addField(sb, DIGEST.jsonKey, getDigest());
        }
        return JsonUtils.endJson(sb).toString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContentType() {
        return contentType;
    }

    public long getLength() {
        return length;
    }

    public long getParts() {
        return parts;
    }

    public long getPartSize() {
        return partSize;
    }

    public long getLastPartSize() {
        return lastPartSize;
    }

    public long getFileDate() {
        return fileDate;
    }

    public FileMeta description(String description) {
        this.description = description;
        return this;
    }

    public FileMeta parts(long parts) {
        this.parts = parts;
        return this;
    }

    public FileMeta partSize(long partSize) {
        this.partSize = partSize;
        return this;
    }

    public FileMeta lastPartSize(long lastPartSize) {
        this.lastPartSize = lastPartSize;
        return this;
    }

    @Override
    public String toString() {
        return "FileMeta" + toJson();
    }
}

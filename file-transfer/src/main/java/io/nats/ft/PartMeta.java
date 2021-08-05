package io.nats.ft;

import io.nats.client.impl.Headers;
import io.nats.client.support.JsonUtils;

import java.util.List;

import static io.nats.ft.Meta.Field.*;

public class PartMeta extends Meta<PartMeta>
{
    private String fileId;
    private long partNumber;
    private long start;
    private String contentEncoding;
    private long encodedLength;

    public PartMeta(FileMeta fm, long partNumber) {
        id = fm.getId() + "-" + partNumber;
        this.partNumber = partNumber;
        start = -1;
        encodedLength = -1;
    }

    public PartMeta(Headers h) {
        id = get(h, ID);
        fileId = get(h, FILE_ID);
        partNumber = getLong(h, PART_NUMBER, -1);
        start = getLong(h, START, -1);
        length = getLong(h, LENGTH, -1);
        encodedLength = getLong(h, ENCODED_LENGTH, -1);
        contentEncoding = get(h, CONTENT_ENCODING);
        parseDigest(get(h, DIGEST));
    }

    public Headers toHeaders() {
        Headers h = new Headers();
        put(h, ID, id);
        put(h, FILE_ID, fileId);
        put(h, PART_NUMBER, partNumber);
        put(h, START, start);
        put(h, LENGTH, length);
        put(h, ENCODED_LENGTH, encodedLength);
        put(h, CONTENT_ENCODING, contentEncoding);
        if (digestAlgorithm != null && digestValue != null) {
            put(h, DIGEST, getDigest());
        }
        return h;
    }

    @Override
    protected PartMeta getThis() {
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public long getPartNumber() {
        return partNumber;
    }

    public long getStart() {
        return start;
    }

    public long getEncodedLength() {
        return encodedLength;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public PartMeta fileUuid(String fileUuid) {
        this.fileId = fileUuid;
        return this;
    }

    public PartMeta partNumber(long partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public PartMeta start(long start) {
        this.start = start;
        return this;
    }

    public PartMeta encodedLength(long encodedLength) {
        this.encodedLength = encodedLength;
        return this;
    }

    public PartMeta contentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
        return this;
    }

    private void put(Headers h, Field key, String s) {
        if (s != null) {
            h.put(key.headerKey, s);
        }
    }

    private void put(Headers h, Field key, long n) {
        if (n > 0) {
            h.put(key.headerKey, Long.toString(n));
        }
    }

    private String get(Headers h, Field key) {
        List<String> list = h.get(key.headerKey);
        return list == null ? null : list.get(0);
    }

    @SuppressWarnings("SameParameterValue")
    private long getLong(Headers h, Field key, long dflt) {
        List<String> list = h.get(key.headerKey);
        return list == null ? dflt : Long.parseLong(list.get(0));
    }

    public String toJson() {
        StringBuilder sb = JsonUtils.beginJson();
        JsonUtils.addField(sb, ID.jsonKey, id);
        JsonUtils.addField(sb, FILE_ID.jsonKey, fileId);
        JsonUtils.addField(sb, PART_NUMBER.jsonKey, partNumber);
        JsonUtils.addField(sb, START.jsonKey, start);
        JsonUtils.addField(sb, LENGTH.jsonKey, length);
        JsonUtils.addField(sb, ENCODED_LENGTH.jsonKey, encodedLength);
        JsonUtils.addField(sb, CONTENT_ENCODING.jsonKey, contentEncoding);
        if (digestAlgorithm != null && digestValue != null) {
            JsonUtils.addField(sb, DIGEST.jsonKey, getDigest());
        }
        return JsonUtils.endJson(sb).toString();
    }

    @Override
    public String toString() {
        return "PartMeta" + toJson();
    }
}

package io.nats.ft;

import io.nats.client.support.JsonUtils;

import java.util.regex.Pattern;

import static io.nats.ft.Constants.META_HEADER_PREFIX;

public abstract class Meta<T>
{
    enum Field {      // headerKey           jsonKey            prefixed  isNumber
        ID(              "id",               "id",              true,     false),
        FILE_ID(         "file-id",          "fileId",          true,     false),
        NAME(            "name",             "name",            true,     false),
        DESCRIPTION(     "desc",             "desc",            true,     false),
        CONTENT_TYPE(    "Content-Type",     "contentType",     false,    false),
        LENGTH(          "length",           "length",          true,     true),
        ENCODED_LENGTH(  "encoded-length",   "encodedLength",   true,     true),
        PARTS(           "parts",            "parts",           true,     true),
        PART_SIZE(       "part-size",        "partSize",        true,     true),
        LAST_PART_SIZE(  "last-part-size",   "lastPartSize",    true,     true),
        FILE_DATE(       "file-date",        "fileDate",        true,     true),
        PART_NUMBER(     "part-number",      "partNumber",      true,     true),
        START(           "start",            "start",           true,     true),
        CONTENT_ENCODING("Content-Encoding", "contentEncoding", false,    false),
        DIGEST(          "Digest",           "digest",          false,    false);

        final String headerKey;
        final String jsonKey;
        final Pattern jsonRe;

        Field(String headerKey, String jsonKey, boolean prefixed, boolean isNumber) {
            this.headerKey = (prefixed ? META_HEADER_PREFIX : "") + headerKey;
            this.jsonKey = jsonKey;
            jsonRe = isNumber ? JsonUtils.number_pattern(jsonKey) : JsonUtils.string_pattern(jsonKey);
        }
    }

    // these are common to FileMeta and PartMeta
    protected String id;
    protected long length;
    protected String digestAlgorithm;
    protected String digestValue;

    protected abstract T getThis();

    public String getId() {
        return id;
    }

    public long getLength() {
        return length;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public T length(long length) {
        this.length = length;
        return getThis();
    }

    public T digestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
        return getThis();
    }

    public T digestValue(String digestValue) {
        this.digestValue = digestValue;
        return getThis();
    }

    public String getDigest() {
        return digestAlgorithm + "=" + digestValue;
    }

    protected void parseDigest(String digest) {
        if (digest != null) {
            String[] split = digest.split("\\Q=\\E");
            digestAlgorithm = split[0];
            digestValue = split[1];
        }
    }
}

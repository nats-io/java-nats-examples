package io.nats.ft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digester {
    private final MessageDigest digest;

    public Digester() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(Constants.DIGEST_ALGORITHM);
    }

    public Digester update(String input) {
        digest.update(input.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public Digester update(long input) {
        return update(Long.toString(input));
    }

    public Digester update(byte[] input) {
        digest.update(input);
        return this;
    }

    public Digester update(byte[] input, int offset, int len) {
        digest.update(input, offset, len);
        return this;
    }

    public Digester reset() {
        digest.reset();
        return this;
    }

    public Digester reset(String input) {
        return reset().update(input);
    }

    public Digester reset(long input) {
        return reset().update(input);
    }

    public Digester reset(byte[] input) {
        return reset().update(input);
    }

    public Digester reset(byte[] input, int offset, int len) {
        return reset().update(input, offset, len);
    }

    public String getDigestValue() {
        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}

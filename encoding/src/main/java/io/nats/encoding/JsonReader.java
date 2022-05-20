package io.nats.encoding;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JsonReader {
    public static ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T read(String json, Class<T> valueType) throws IOException {
        return MAPPER.readValue(json, valueType);
    }

    public static <T> T read(byte[] jsonBytes, Class<T> valueType) throws IOException {
        return MAPPER.readValue(jsonBytes, valueType);
    }

    public static <T> T read(File file, Class<T> valueType) throws IOException {
        return read(Files.readAllBytes(file.toPath()), valueType);
    }

    public static <T> T readFromFile(String filename, Class<T> valueType) throws IOException {
        return read(new File(filename), valueType);
    }
}

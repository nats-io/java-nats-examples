package io.nats.jsmulti.internal;

public interface Publisher<T> {
    T publish(byte[] payload) throws Exception;
}

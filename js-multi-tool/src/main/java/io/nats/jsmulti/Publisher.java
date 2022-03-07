package io.nats.jsmulti;

interface Publisher<T> {
    T publish(byte[] payload) throws Exception;
}

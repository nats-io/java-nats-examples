package io.nats.jsmulti;

import io.nats.client.Connection;
import io.nats.client.JetStream;

interface Runner {
    void run(Connection nc, JetStream js, Stats stats, int id) throws Exception;
}

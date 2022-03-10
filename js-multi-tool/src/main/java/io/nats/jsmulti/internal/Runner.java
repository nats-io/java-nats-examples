package io.nats.jsmulti.internal;

import io.nats.client.Connection;
import io.nats.jsmulti.shared.Stats;

public interface Runner {
    void run(Connection nc, Stats stats, int id) throws Exception;
}

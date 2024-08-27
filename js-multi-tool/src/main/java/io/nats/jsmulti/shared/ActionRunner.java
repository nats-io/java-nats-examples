package io.nats.jsmulti.shared;

import io.nats.client.Connection;
import io.nats.jsmulti.settings.Context;

// ----------------------------------------------------------------------------------------------------
// Runners
// ----------------------------------------------------------------------------------------------------
public interface ActionRunner {
    void run(Context ctx, Connection nc, Stats stats, int id) throws Exception;
}

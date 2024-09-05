// Copyright 2021-2024 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.jsmulti.shared;

import io.nats.client.*;
import io.nats.jsmulti.settings.Context;

import java.time.Duration;

public interface OptionsFactory {
    enum OptionsType { ADMIN, DEFAULT}

    default Options.Builder getOptionsBuilder(Context ctx, OptionsType ot) {
        String server = getServer(ctx, ot);
        return new Options.Builder()
            .server(server)
            .authHandler(getAuthHandler(ctx))
            .connectionTimeout(getConnectionTimeout(ctx))
            .reconnectWait(getReconnectWait(ctx))
            .errorListener(getErrorListener());
    }

    default Options getOptions(Context ctx) {
        return getOptionsBuilder(ctx, OptionsType.DEFAULT).build();
    }

    default Options getOptions(Context ctx, OptionsType ot) {
        return getOptionsBuilder(ctx, ot).build();
    }

    default String getServer(Context ctx) {
        return getServer(ctx, OptionsType.DEFAULT);
    }

    default String getServer(Context ctx, OptionsType ot) {
        return ot == OptionsType.ADMIN ? ctx.getFirstServer() : ctx.getNextServer();
    }

    default AuthHandler getAuthHandler(Context ctx) {
        return ctx.credsFile == null ? null : Nats.credentials(ctx.credsFile);
    }

    default Duration getConnectionTimeout(Context ctx) {
        return Duration.ofMillis(ctx.connectionTimeoutMillis);
    }

    default Duration getReconnectWait(Context ctx) {
        return Duration.ofMillis(ctx.reconnectWaitMillis);
    }

    default ErrorListener getErrorListener() {
        return new ErrorListener() {};
    }

    default JetStreamOptions getJetStreamOptions(Context ctx) throws Exception {
        return JetStreamOptions.DEFAULT_JS_OPTIONS;
    }
}

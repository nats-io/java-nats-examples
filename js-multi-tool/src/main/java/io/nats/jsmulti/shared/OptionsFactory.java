// Copyright 2021-2022 The NATS Authors
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

import io.nats.client.ErrorListener;
import io.nats.client.JetStreamOptions;
import io.nats.client.Options;
import io.nats.jsmulti.settings.Context;

import java.time.Duration;

public interface OptionsFactory {
    default Options getOptions(Context ctx) throws Exception {
        return getOptions(ctx.server);
    }

    default JetStreamOptions getJetStreamOptions(Context ctx) throws Exception {
        return JetStreamOptions.DEFAULT_JS_OPTIONS;
    }

    static Options getOptions(String server) {
        return new Options.Builder()
            .server(server)
            .connectionTimeout(Duration.ofSeconds(5))
            .pingInterval(Duration.ofSeconds(10))
            .reconnectWait(Duration.ofSeconds(1))
            .errorListener(new ErrorListener() {})
            .build();
    }

    static JetStreamOptions getJetStreamOptions() {
        return JetStreamOptions.DEFAULT_JS_OPTIONS;
    }
}

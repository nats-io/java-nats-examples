// Copyright 2022 The NATS Authors
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

package io.nats.jsmulti.examples;

import io.nats.client.*;
import io.nats.client.api.StreamConfiguration;
import io.nats.jsmulti.settings.Context;
import io.nats.jsmulti.settings.StreamOptions;

public class StreamUtils {

    public static void setupStream(String stream, Context context) throws Exception {
        setupStream(stream, context.subject, new StreamOptions(), context.getOptions(), context.getJetStreamOptions());
    }

    public static void setupStream(String stream, StreamOptions so, Context context) throws Exception {
        setupStream(stream, context.subject, so, context.getOptions(), context.getJetStreamOptions());
    }

    public static void setupStream(String stream, String subject, StreamOptions so, Options options, JetStreamOptions jso) throws Exception {
        try (Connection nc = Nats.connect(options)) {
            JetStreamManagement jsm = nc.jetStreamManagement(jso);
            try { jsm.deleteStream(stream); } catch (JetStreamApiException ignored) {}
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(stream)
                .subjects(subject)
                .storageType(so.storageType)
                .maxBytes(so.maxBytes)
                .replicas(so.replicas)
                .build();
            System.out.println("CREATED: " + jsm.addStream(streamConfig));
        }
    }
}

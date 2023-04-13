// Copyright 2023 The NATS Authors
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

package io.nats;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;

public class Utils {
    public static final String STREAM = "TheStream";
    public static final String SUBJECT = "TheSubject";
    public static final String SYNC_CONSUMER = "SyncConsumer";
    public static final String CALLBACK_CONSUMER = "CallbackConsumer";

    public static void createTestStream(JetStreamManagement jsm) throws IOException, JetStreamApiException {
        try {
            jsm.deleteStream(STREAM);
        }
        catch (Exception ignore) {}

        StreamConfiguration sc = StreamConfiguration.builder()
            .name(STREAM)
            .storageType(StorageType.Memory)
            .subjects(SUBJECT)
            .build();
        jsm.addStream(sc);
    }
}

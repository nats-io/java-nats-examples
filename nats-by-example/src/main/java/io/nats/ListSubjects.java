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
import io.nats.client.api.*;
import io.nats.client.impl.Headers;

import java.io.IOException;
import java.util.List;

/**
 * Demonstrate how to list subjects for a stream.
 */
public class ListSubjects {
    public static void main(String[] args) {
        String natsURL = System.getenv("NATS_URL");
        if (natsURL == null) {
            natsURL = "nats://127.0.0.1:4222";
        }

        try (Connection nc = Nats.connect(natsURL)) {
            JetStreamManagement jsm = nc.jetStreamManagement();
            JetStream js = jsm.jetStream();

            // Create a stream with a few subjects
            jsm.addStream(StreamConfiguration.builder()
                .name("subjects")
                .subjects("plain", "greater.>", "star.*")
                .build());

            // ### GetStreamInfo with StreamInfoOptions
            // Get the subjects via the getStreamInfo call.
            // Since this is "state" there are no subjects in the state unless
            // there are messages in the subject.
            StreamInfo si = jsm.getStreamInfo("subjects", StreamInfoOptions.allSubjects());
            StreamState state = si.getStreamState();
            System.out.println("Before publishing any messages, there are 0 subjects: " + state.getSubjectCount());

            // Publish a message
            js.publish("plain", null);

            si = jsm.getStreamInfo("subjects", StreamInfoOptions.allSubjects());
            state = si.getStreamState();
            System.out.println("After publishing a message to a subject, it appears in state:");
            for (Subject s : state.getSubjects()) {
                System.out.println("  "  + s);
            }

            // Publish some more messages, this time against wildcard subjects
            js.publish("greater.A", null);
            js.publish("greater.A.B", null);
            js.publish("greater.A.B.C", null);
            js.publish("greater.B.B.B", null);
            js.publish("star.1", null);
            js.publish("star.2", null);

            si = jsm.getStreamInfo("subjects", StreamInfoOptions.allSubjects());
            state = si.getStreamState();
            System.out.println("Wildcard subjects show the actual subject, not the template.");
            for (Subject s : state.getSubjects()) {
                System.out.println("  "  + s);
            }

            // ### Subject Filtering
            // Instead of allSubjects, you can filter for a specific subject
            si = jsm.getStreamInfo("subjects", StreamInfoOptions.filterSubjects("greater.>"));
            state = si.getStreamState();
            System.out.println("Filtering the subject returns only matching entries ['greater.>']");
            for (Subject s : state.getSubjects()) {
                System.out.println("  "  + s);
            }

            si = jsm.getStreamInfo("subjects", StreamInfoOptions.filterSubjects("greater.A.>"));
            state = si.getStreamState();
            System.out.println("Filtering the subject returns only matching entries ['greater.A.>']");
            for (Subject s : state.getSubjects()) {
                System.out.println("  "  + s);
            }
        } catch (InterruptedException | IOException | JetStreamApiException e) {
            e.printStackTrace();
        }
    }
}

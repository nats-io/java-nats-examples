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

public class Usage {

    public static void main(String[] args) {
        usage();
    }

    public static void usage() {
        System.out.println(Usage.USAGE);
    }

    public static final String ACTIONS = "-a action (string), required, one of "
        + "\n   PubSync      - publish synchronously"
        + "\n   PubAsync     - publish asynchronously"
        + "\n   PubCore      - core publish (synchronously) to subject"
        + "\n   SubPush      - push subscribe read messages (synchronously)"
        + "\n   SubQueue     - push subscribe read messages with queue (synchronously)."
        + "\n                    Requires 2 or more threads"
        + "\n   SubPull      - pull subscribe fetch messages"
        + "\n   SubPullQueue - pull subscribe fetch messages, queue (using common durable)"
        + "\n   SubPullRead  - pull subscribe read messages"
        + "\n   SubPullReadQueue - pull subscribe read messages, queue (using common durable)"
        + "\n                    Requires 2 or more threads"
        + "\n   RTT          - round trip timing"
        + "\n   Pub          - core publish"
        + "\n   Request      - core request"
        + "\n   Reply        - core reply"
        + "\n   SubCore      - core push subscribe read messages (synchronously)"
        + "\n   SubCoreQueue - core push subscribe read messages with queue (synchronously)."
        ;

    public static final String SERVER = "-s server url (string), optional, defaults to nats://localhost:4222"
        + "\n-cf credentials file (string), optional"
        + "\n-ctms connection timeout millis, optional, defaults to 5000"
        + "\n-rwms reconnect wait millis, optional, defaults to 1000"
        + "\n-of options factory class name. Class with no op constructor that implements OptionsFactory"
        + "\n    If supplied, used instead of -s, -cf, -ctms and -rwms."
        + "\n-rf report frequency (number) how often to print progress, defaults to 10% of message count."
        + "\n    <= 0 for no reporting. Reporting time is excluded from timings";

    public static final String LATENCY =
        "-lf latency flag. Needed when publishing to test latency. See examples."
        + "\n-lcsv latency-csv-file-spec";

    public static final String NOTES = "All text constants are case insensitive, i.e."
        + "\n  action, connection strategy, ack policy, pull type"
        + "\nInput numbers can be formatted for easier viewing. For instance, ten thousand"
        + "\n  can be any of these: 10000 10,000 10.000 10_000"
        + "\n  and can end with factors k, m, g meaning x 1000, x 1_000_000, x 1_000_000_000"
        + "\n  or ki, mi, gi meaning x 1024, x 1024 * 1024, x 1024 * 1024 * 1024"
        + "\nUse tls:// or opentls:// in the server url to require tls, via the Default SSLContext";

    public static final String GENERAL = "-u subject (string), required for publishing or subscribing"
        + "\n-m message count (number) required > 1 for publishing or subscribing, defaults to 100_000"
        + "\n-d threads (number) for publishing or subscribing, defaults to 1"
        + "\n-n connection strategy (shared|individual) when threading, whether to share"
        + "\n     the connection, defaults to shared"
        + "\n-j jitter (number) between publishes or subscribe message retrieval of random"
        + "\n     number from 0 to j-1, in milliseconds, defaults to 0 (no jitter), maximum 10_000"
        + "\n     time spent in jitter is excluded from timings"
        + "\n-ps payload size (number) for publishing, defaults to 128, maximum 1048576"
        + "\n-rs round size (number) for pubAsync, default to 100, maximum 1000"
        + "\n-kp ack policy (explicit|none|all) for subscriptions, defaults to explicit"
        + "\n-kf ack all frequency (number), applies to ack policy all, ack after kf messages"
        + "\n      defaults to 1, maximum 100"
        + "\n-bs batch size (number) for subPull*, defaults to 10, maximum 200"
        + "\n-rqwms request wait millis, time to wait for any JetStream request to complete, default is 1000 milliseconds"
        + "\n-rtoms read timeout wait millis, time to wait for an individual synchronous message read (next or fetch), default is 1000 milliseconds"
        + "\n-rmxwms read max wait millis, when reading messages in a loop, stop if there are no messages in this time, default is 10 seconds (10000ms)";

    public static final String SSEP =
        "---------------------------";

    public static String section(String label, String body) {
        return "\n\n" + label + "\n" + SSEP.substring(0, label.length()) + "\n" + body;
    }

    public static final String USAGE = "\nUSAGE:"
        + section("Actions Arguments", ACTIONS)
        + section("Server Arguments", SERVER)
        + section("Latency Arguments", LATENCY)
        + section("General Arguments", GENERAL)
        + section("Notes", NOTES)
        ;
}

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

public class Usage {

    public static void main(String[] args) {
        usage();
    }

    public static void usage() {
        System.out.println(Usage.USAGE);
    }

    public static final String USAGE =
        "\nUsage: "
            + "\n  gradle clean jsMulti --args=\"[configuration options]\""
            + "\n  mvn clean compile exec:java -Dexec.mainClass=\"io.nats.jsmulti.JsMulti\" -Dexec.args=\"[configuration options]\""
            + "\n  java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.JsMulti [configuration options]"
            + "\n---------------------------------------------------------------------------------------"
            + "\nConfiguration Options"
            + "\n---------------------------------------------------------------------------------------"
            + "\n-a action (string), required, one of "
            + "\n   pubSync      - publish synchronously"
            + "\n   pubAsync     - publish asynchronously"
            + "\n   pubCore      - core publish (synchronously) to subject"
            + "\n   subPush      - push subscribe read messages (synchronously)"
            + "\n   subQueue     - push subscribe read messages with queue (synchronously)."
            + "\n                    Requires 2 or more threads"
            + "\n   subPull      - pull subscribe read messages"
            + "\n   subPullQueue - pull subscribe read messages, queue (using common durable)"
            + "\n                    Requires 2 or more threads"
            + "\n---------------------------------------------------------------------------------------"
            + "\n-s server url (string), optional, defaults to nats://localhost:4222"
            + "\n-cf credentials file (string), optional"
            + "\n-ctms connection timeout millis, optional, defaults to 5000"
            + "\n-rwms reconnect wait millis, optional, defaults to 1000"
            + "\n-of options factory class name. Class with no op constructor that implements OptionsFactory"
            + "\n    If supplied, used instead of -s, -cf, -ctms and -rwms."
            + "\n---------------------------------------------------------------------------------------"
            + "\n-rf report frequency (number) how often to print progress, defaults to 10% of message count."
            + "\n    <= 0 for no reporting. Reporting time is excluded from timings"
            + "\n-lf latency flag. Needed when publishing to test latency. See examples."
            + "\n---------------------------------------------------------------------------------------"
            + "\n-u subject (string), required for publishing or subscribing"
            + "\n-m message count (number) required > 1, used with both publishing or subscribing, defaults to 100_000"
            + "\n-d threads (number) for publishing or subscribing, defaults to 1"
            + "\n-n connection strategy (shared|individual) when threading, whether to share"
            + "\n     the connection, defaults to shared"
            + "\n-j jitter (number) between publishes or subscribe message retrieval of random"
            + "\n     number from 0 to j-1, in milliseconds, defaults to 0 (no jitter), maximum 10_000"
            + "\n     time spent in jitter is excluded from timings"
            + "\n---------------------------------------------------------------------------------------"
            + "\n-ps payload size (number) for publishing, defaults to 128, maximum 1048576"
            + "\n-rs round size (number) for pubAsync, default to 100, maximum 1000"
            + "\n---------------------------------------------------------------------------------------"
            + "\n-kp ack policy (explicit|none|all) for subscriptions, defaults to explicit"
            + "\n-kf ack all frequency (number), applies to ack policy all, ack after kf messages"
            + "\n      defaults to 1, maximum 100"
            + "\n---------------------------------------------------------------------------------------"
            + "\n-bs batch size (number) for subPull/subPullQueue, defaults to 10, maximum 200"
            + "\n---------------------------------------------------------------------------------------"
            + "\n All text constants are case insensitive, i.e."
            + "\n  action, connection strategy, ack policy, pull type"
            + "\nInput numbers can be formatted for easier viewing. For instance, ten thousand"
            + "\n  can be any of these: 10000 10,000 10.000 10_000"
            + "\nUse tls:// or opentls:// in the server url to require tls, via the Default SSLContext"
            + "\n---------------------------------------------------------------------------------------"
        ;
}

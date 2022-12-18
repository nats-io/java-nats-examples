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

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;

/**
 * Example class running a producer (publisher)
 *
 * Various ways to run the code
 * 1. Through an ide...
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Producer -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean producer --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `producer` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Producer [args]
 *    ! You must have run gradle clean jar and know where the jnats library is
 * 5. Command Line: java -cp <path-to-uber-jar> io.nats.jsmulti.examples.Producer [args]
 *    ! You must have run gradle clean uberJar
 */
public class Producer {

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    static final boolean LATENCY_RUN = false;

    public static void main(String[] args) throws Exception {
        // You could code this to use args to create the Arguments
        Arguments a = Arguments.instance()
            .server(SERVER)
            .subject(SUBJECT)
            .action(Action.PUB_SYNC)    // or Action.PUB_ASYNC or Action.PUB_CORE for example
            .latencyFlag(LATENCY_RUN)   // tells the code to add latency info to the header
            .messageCount(50_000)       // default is 100_000
            .payloadSize(256)           // default is 128
            .roundSize(50)              // how often to check Async Publish Acks, default is 100
            .threads(3)                 // default is 1
            .individualConnection()     // versus .sharedConnection()
            // .reportFrequency(500)    // default is 10% of message count
            ;

        a.printCommandLine();

        Context ctx = new Context(a);

        // ---------------------------------------------------
        // For latency runs, the consumer code sets up the
        // stream because the stream needs to exist before the
        // consumers start.
        // ---------------------------------------------------
        if (!LATENCY_RUN) {
            StreamUtils.setupStream(STREAM, ctx);
        }

        JsMulti.run(ctx);
    }
}

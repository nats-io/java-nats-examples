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
 * Example class running a consumer
 *
 * Various ways to run the code
 * 1. Through an ide...
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Consumer -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean consumer --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `consumer` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Consumer [args]
 *    ! You must have run gradle clean jar and know where the jnats library is
 * 5. Command Line: java -cp <path-to-uber-jar> io.nats.jsmulti.examples.Consumer [args]
 *    ! You must have run gradle clean uberJar
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        //        while (true) {
//            main("nats://localhost:4222");
//            Thread.sleep(10_000);
//            System.gc();
//            Thread.sleep(5_000);
//        }

        while (true) {
            Thread t4 = new Thread(() -> main("nats://localhost:4222"));
            t4.start();
            Thread t5 = new Thread(() -> main("nats://localhost:5222"));
            t5.start();
            Thread t6 = new Thread(() -> main("nats://localhost:6222"));
            t6.start();

            t4.join();
            t5.join();
            t6.join();

            Thread.sleep(10_000);
        }
    }

    private static void main(String server) {
        Arguments a = Arguments.instance()
            .server(server)
            .subject("sub")
            .action(Action.SUB_PULL)
            .messageCount(1_000_000)
            .ackExplicit()
//            .ackPolicy(AckPolicy.All)
//            .ackAllFrequency(50)
            .batchSize(100)
            .individualConnection()
            .reportFrequency(25_000)
            ;
        a.printCommandLine();
        Context ctx = new Context(a);
        try {
            JsMulti.run(ctx, false, true);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

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
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Rtt -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean rtt --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `rtt` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Rtt [args]
 *    ! You must have run gradle clean jar and know where the jnats library is
 * 5. Command Line: java -cp <path-to-uber-jar> io.nats.jsmulti.examples.Rtt [args]
 *    ! You must have run gradle clean uberJar
 */
public class Rtt {

    static final String SERVER = "tls://gcp.cloud.ngs.global"; // "nats://localhost:4222";

    public static void main(String[] args) throws Exception {
        // You could code this to use args to create the Arguments
        Arguments a = Arguments.instance()
            .credsFile("C:\\Users\\batman\\.local\\share\\nats\\nsc\\keys\\creds\\synadia\\remote_control\\default.creds")
            .server(SERVER)
            .action(Action.RTT)
            .messageCount(10_000) // default is 100_000
            .add(args)            // last added wins so these will take precedence over defaults
            ;

        a.printCommandLine();

        Context ctx = new Context(a);

        JsMulti.run(ctx);
    }
}

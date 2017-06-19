package io.nats.jnats;

import io.nats.client.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Test the Basic Usage example code.
     */
    public void testBasicUsage()
    {
        try {

            // Connect to default URL ("nats://localhost:4222")
            Connection nc = Nats.connect();

            // Simple Publisher
            nc.publish("foo", "Hello World".getBytes());

            // Simple Async Subscriber
            nc.subscribe("foo", m -> {
                System.out.printf("Received a message: %s\n", new String(m.getData()));
            });

            // Simple Sync Subscriber
            int timeout = 1000;
            SyncSubscription sub = nc.subscribeSync("foo");
            Message msg = sub.nextMessage(timeout);

            // Unsubscribing
            sub = nc.subscribe("foo");
            sub.unsubscribe();

            // Requests
            msg = nc.request("help", "help me".getBytes(), 10000);

            // Replies
            nc.subscribe("help", reply -> {
                try {
                    nc.publish(reply.getReplyTo(), "I can help!".getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Close connection
            nc.close();
        }
        catch (Exception e) {
             e.printStackTrace();
        }

    }

}

package io.nats.example;

import io.nats.client.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
Multiple replies to one request
 */
public class MultiReplyOneReq
{
    public static class Worker implements Runnable {
        Connection nc;
        int id;

        public Worker(Connection nc, int id) {
            this.nc = nc;
            this.id = id;
        }

        @Override
        public void run() {
            Subscription sub = nc.subscribe("request-for-worker");
            try {
                Message m = sub.nextMessage(10000); // a long wait here which is simulating listening forever
                System.out.println("Worker " + id + " responding.");
                nc.publish(m.getReplyTo(), ("Response from worker " + id).getBytes());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {

            // Start the workers first. They have to be subscribed before messages get published
            for (int workerId = 1; workerId <= 5; workerId++) {
                new Thread(new Worker(nc, workerId)).start();
            }

            // Start the requester. It must be subscribed before responses get published
            CountDownLatch latch = new CountDownLatch(5); // wait for 5 responses or timeout (see below)
            Dispatcher d = nc.createDispatcher(msg -> {
                System.out.printf("Received message \"%s\" on subject '%s'\n",
                    new String(msg.getData(), StandardCharsets.UTF_8),
                    msg.getSubject());
                latch.countDown();
            });
            d.subscribe("worker-responses");

            // publish the primary message
            nc.publish("request-for-worker", "worker-responses", "this is the data".getBytes());

            // the latch will stop waiting once there are 5 messages or 5 seconds
            latch.await(5, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

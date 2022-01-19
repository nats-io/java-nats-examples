// Copyright 2021 The NATS Authors
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

package io.nats.example;

import io.nats.client.*;
import io.nats.client.support.RandomUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
Multiple replies to one request
 */
public class MultipleRepliesForOneRequest
{
    public static int WORKER_COUNT = 5;

    private static String extractRequestIdFromSubject(Message msg) {
        int at = msg.getSubject().lastIndexOf(".");
        return msg.getSubject().substring(at + 1);
    }

    /**
     * Worker responds to requests of some work type, in this example TaskTypeA
     * There will be multiple instances of this worker and they all will respond to the same message.
     */
    public static class Worker implements Runnable {
        Connection nc;
        int id;

        public Worker(Connection nc, int id) {
            this.nc = nc;
            this.id = id;
        }

        @Override
        public void run() {
            Subscription sub = nc.subscribe("Request.TaskTypeA.*");
            try {
                Message msg = sub.nextMessage(5000); // a long wait here is simulating listening forever

                // Do some work with that message...
                String requestId = extractRequestIdFromSubject(msg);
                System.out.println(System.currentTimeMillis() + ": Worker " + id + " responding to request " + requestId);

                // ... then publish to the replyTo, just like regular reply-request
                nc.publish(msg.getReplyTo(), ("worker-" + id + " worked on " + requestId).getBytes());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Originator will
     * 1. publish something that multiple workers will respond to
     * 2. handle the responses from the workers
     */
    public static class Originator implements Runnable {
        Connection nc;
        String requesterId;
        CountDownLatch latch;

        public Originator(Connection nc) {
            this.nc = nc;
            this.requesterId = Long.toHexString(RandomUtils.PRAND.nextLong());
            latch = new CountDownLatch(WORKER_COUNT); // will latch.await( for )WORKER_COUNT in the run below)

            Dispatcher d = nc.createDispatcher(msg -> {
                System.out.printf("%d: Originator received \"%s\" in response to %s\n",
                    System.currentTimeMillis(),
                    new String(msg.getData(), StandardCharsets.UTF_8),
                    extractRequestIdFromSubject(msg));
                latch.countDown();
            });

            d.subscribe("Response." + requesterId + ".>"); // listens to all for the Responses for the requester id
        }

        @Override
        public void run() {
            // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
            boolean keepGoing = true;
            while (keepGoing) {

                // 1. Do whatever work you want to do
                String taskType = "TaskTypeA";
                String requestId = "rqst14273";

                // 2. Publish to the task workers.
                String subject = "Request." + taskType + "." + requestId;                      // Request.TaskTypeA.requestId111
                String replyTo = "Response." + requesterId + "." + taskType + "." + requestId; // Response.<requesterId>.TaskTypeA.requestId111
                nc.publish(subject, replyTo, "this is the task data".getBytes());

                // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                try {
                    latch.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                keepGoing = false;
            }
        }
    }

    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {

            // Start the workers first. They have to be subscribed before messages get published
            for (int workerId = 1; workerId <= WORKER_COUNT; workerId++) {
                new Thread(new Worker(nc, workerId)).start();
            }

            // Start the originator and let it run.
            Thread t = new Thread(new Originator(nc));
            t.start();
            t.join(2000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

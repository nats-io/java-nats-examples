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
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
Multiple example of both types of workers.
 */
public class ComboSample
{
    public static int WORKER_A_COUNT = 5;
    public static int WORKER_B_COUNT = 3;

    private static String extractTaskTypeFromSubject(Message msg) {
        int at = msg.getSubject().lastIndexOf(".");
        String temp = msg.getSubject().substring(0, at);
        at = temp.lastIndexOf(".");
        return temp.substring(at + 1);
    }

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
        String id;
        String taskType;
        boolean queue;

        public Worker(Connection nc, String id, String taskType, boolean queue) {
            this.nc = nc;
            this.id = id;
            this.taskType = taskType;
            this.queue = queue;
        }

        @Override
        public void run() {
            Subscription sub;
            if (queue) {
                sub = nc.subscribe("Request." + taskType + ".*", "Q" + taskType);
            }
            else {
                sub = nc.subscribe("Request." + taskType + ".*");
            }
            try {
                Message msg = sub.nextMessage(2000); // a long wait here is simulating listening forever

                if (msg != null) {
                    // Do some work with that message...
                    String taskType = extractTaskTypeFromSubject(msg);
                    String requestId = extractRequestIdFromSubject(msg);
                    System.out.println(System.currentTimeMillis() + ": Worker " + id + " responding to request " + requestId + " for " + taskType);

                    // ... then publish to the replyTo, just like regular reply-request
                    nc.publish(msg.getReplyTo(), ("worker-" + id + " worked on " + requestId + " for " + taskType).getBytes());
                }

            } catch (Exception e) {
                System.out.println(System.currentTimeMillis() + ": Worker " + id + " " + e);
            }
        }
    }

    /**
     * Originator will
     * 1. publish something that multiple workers will respond to
     * 2. handle the responses from the workers
     */
    public static class OriginatorA implements Runnable {
        Connection nc;
        String requesterId;
        CountDownLatch latch;

        public OriginatorA(Connection nc, int workers) {
            this.nc = nc;
            this.requesterId = Long.toHexString(RandomUtils.PRAND.nextLong());
            latch = new CountDownLatch(workers); // will latch.await( for )WORKER_COUNT in the run below)

            Dispatcher d = nc.createDispatcher(msg -> {
                System.out.printf("%d: OriginatorA received \"%s\" in response to %s\n",
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
                String requestId = "A14273";

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


    /**
     * Originator will
     * 1. publish something that multiple queue workers are available to respond to
     * 2. handle the response from the workers
     */
    public static class OriginatorB implements Runnable {
        Connection nc;

        public OriginatorB(Connection nc) {
            this.nc = nc;
        }

        @Override
        public void run() {
            // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
            boolean keepGoing = true;
            while (keepGoing) {

                // 1. Do whatever work you want to do
                String taskType = "TaskTypeB";
                String requestId = "B98765";

                // 2. Publish to the task workers.
                String subject = "Request." + taskType + "." + requestId; // Request.TaskTypeA.requestId111

                // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                try {
                    Message msg = nc.request(subject, "this is the task data".getBytes(), Duration.ofSeconds(2));
                    System.out.printf("%d: OriginatorB received \"%s\" in response\n", System.currentTimeMillis(), new String(msg.getData(), StandardCharsets.UTF_8));
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
            for (int workerId = 1; workerId <= WORKER_A_COUNT; workerId++) {
                new Thread(new Worker(nc, "A" + workerId, "TaskTypeA", false)).start();
            }

            for (int workerId = 1; workerId <= WORKER_B_COUNT; workerId++) {
                new Thread(new Worker(nc, "B" + workerId, "TaskTypeB", true)).start();
            }

            // Start the originators
            Thread ta = new Thread(new OriginatorA(nc, WORKER_A_COUNT));
            ta.start();

            Thread tb = new Thread(new OriginatorB(nc));
            tb.start();

            ta.join(2000);
            tb.join(2000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

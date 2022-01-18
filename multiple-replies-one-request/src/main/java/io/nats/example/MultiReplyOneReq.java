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
            Subscription sub = nc.subscribe("workers.partition42.*");
            try {
                Message msg = sub.nextMessage(5000); // a long wait here is simulating listening forever
                System.out.println("Worker " + id + " responding.");

                // The requester put a reply-to when it published, so we know where to respond to.
                nc.publish(msg.getReplyTo(), ("Response from worker " + id).getBytes());
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
            d.subscribe("responses.partition42");

            // publish the primary message
            nc.publish("workers.partition42.uniqueId999", "responses.partition42", "this is the data".getBytes());

            // the latch will stop waiting once there are 5 messages or 5 seconds
            latch.await(5, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

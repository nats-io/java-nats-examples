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

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.impl.Headers;

/*
Scatter Gather. A simple version of multiple replies to one request.
 */
public class ScatterGather
{
    public static void main( String[] args )
    {
        try (Connection requesterConn = Nats.connect("nats://localhost:4222");
             Connection responderConn1 = Nats.connect("nats://localhost:4222");
             Connection responderConn2 = Nats.connect("nats://localhost:4222"))
        {
            Dispatcher d1 = responderConn1.createDispatcher(msg -> {
                System.out.println("Responder A replying to request " + new String(msg.getData()) + " via subject '" + msg.getReplyTo() + "'");
                Headers h = new Headers().put("responderId", "A");
                responderConn1.publish(msg.getReplyTo(), h, msg.getData());
            });
            d1.subscribe("scatter");

            Dispatcher d2 = responderConn1.createDispatcher(msg -> {
                System.out.println("Responder B replying to request " + new String(msg.getData()) + " via subject '" + msg.getReplyTo() + "'");
                Headers h = new Headers().put("responderId", "B");
                responderConn2.publish(msg.getReplyTo(), h, msg.getData());
            });
            d2.subscribe("scatter");

            Dispatcher d = requesterConn.createDispatcher(msg -> {
                String mId = new String(msg.getData());
                String responderId = msg.getHeaders().getFirst("responderId");
                System.out.println("Response gathered for message " + mId + " received from responderId " + responderId + ".");
            });
            d.subscribe("gather");

            for (int x = 1; x <= 9; x++) {
                System.out.println("\nPublishing scatter request #" + x);
                requesterConn.publish("scatter", "gather", ("" + x).getBytes());
                Thread.sleep(100); // give request time to work
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

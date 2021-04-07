package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class App {
    // CHANGE THE SERVER URL AS YOU NEED
    static String SERVER_URL = "nats://localhost:4222"; // "nats://demo.nats.io:4222";

    // Try publish style vs request style to see how they differ
    static boolean PUBLISH_STYLE_NOT_REQUEST_STYLE = true;

    public static void main(String[] args) {
        try {
            List<Endpoint> endpoints = new ArrayList<>();
            if (PUBLISH_STYLE_NOT_REQUEST_STYLE) {
                endpoints.add(new PublishStyleWorkers.WorkerA(1));
                endpoints.add(new PublishStyleWorkers.WorkerA(2));
                endpoints.add(new PublishStyleWorkers.WorkerB(1));
                endpoints.add(new PublishStyleWorkers.WorkerB(2));
                endpoints.add(new PublishStyleWorkers.WorkerB(3));
            }
            else {
                endpoints.add(new RequestStyleWorkers.WorkerA(1));
                endpoints.add(new RequestStyleWorkers.WorkerA(2));
                endpoints.add(new RequestStyleWorkers.WorkerB(1));
                endpoints.add(new RequestStyleWorkers.WorkerB(2));
                endpoints.add(new RequestStyleWorkers.WorkerB(3));
            }
            for (Endpoint e : endpoints) {
                System.out.println("Worker " + e.endpointId + " of type " +
                        e.getClass().getCanonicalName().replace("io.nats.examples.", "") + " started.");
            }

            int transactionId = 10000;
            try (Connection nc = Nats.connect(SERVER_URL)) {
                while (true) {
                    Input input = new Input();
                    Headers headers = new Headers();
                    headers.put("transactionId", "" + (++transactionId));
                    headers.put("aId", "" + input.aId);
                    headers.put("bId", "" + input.bId);
                    NatsMessage message = NatsMessage.builder().subject("A" + input.aId + ".step1").headers(headers).build();

                    if (PUBLISH_STYLE_NOT_REQUEST_STYLE) {
                        System.out.println("Publish Style starting for " + input + ", transaction " + transactionId + ". Publishing step 1 on A" + input.aId);
                        nc.publish(message);
                    }
                    else {
                        System.out.println("Request Style starting for " + input + ", transaction " + transactionId + ". Requesting step 1 on A" + input.aId);
                        Message response = nc.request(message, Duration.ofSeconds(2));
                        System.out.println("Request Style starter received a response: " + response);
                    }
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
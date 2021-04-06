package io.nats.examples;

import io.nats.client.*;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class App {
    static String SERVER_URL = Options.DEFAULT_URL; // "nats://demo.nats.io:4222";

    public static void main(String[] args) {
        try {
            new WorkerA(1);
            new WorkerA(2);
            new WorkerB(1);
            new WorkerB(2);
            new WorkerB(3);
            System.out.println();

            int transactionId = 10000;
            try (Connection nc = Nats.connect(SERVER_URL)) {
                while (true) {
                    Input input = gatherInput();
                    Headers headers = new Headers();
                    headers.put("transactionId", "" + (++transactionId));
                    headers.put("aId", "" + input.aId);
                    headers.put("bId", "" + input.bId);
                    NatsMessage message = NatsMessage.builder().subject("A" + input.aId + ".step1").headers(headers).build();
                    nc.publish(message);
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class WorkerA extends Endpoint {
        public WorkerA(int id) throws IOException, InterruptedException, TimeoutException {
            super(id, "A");
        }

        @Override
        protected void command(String cmd, Message msg, String transactionId, String aId, String bId) {
            if (cmd.equals("step1")) {
                System.out.println("Worker " + endpointId + " step 1 processing transaction " + transactionId + ". Calling step 1 on " + bId);
                NatsMessage message = NatsMessage.builder().subject("B" + bId + ".step1").headers(msg.getHeaders()).build();
                nc.publish(message);
            }
            else if (cmd.equals("step2")) {
                System.out.println("Worker " + endpointId + " step 2 processing transaction " + transactionId + ". Calling step 2 on " + bId);
                NatsMessage message = NatsMessage.builder().subject("B" + bId + ".step2").headers(msg.getHeaders()).build();
                nc.publish(message);
            }
        }
    }

    static class WorkerB extends Endpoint {

        public WorkerB(int id) throws IOException, InterruptedException, TimeoutException {
            super(id, "B");
        }

        @Override
        protected void command(String cmd, Message msg, String transactionId, String aId, String bId) {
            if (cmd.equals("step1")) {
                System.out.println("Worker " + endpointId + " step 1 processing transaction " + transactionId + ". Calling step 2 on " + aId);
                NatsMessage message = NatsMessage.builder().subject("A" + aId + ".step2").headers(msg.getHeaders()).build();
                nc.publish(message);
            }
            else if (cmd.equals("step2")) {
                System.out.println("Worker " + endpointId + " step 2 transaction completed.\n");
            }
        }
    }

    static abstract class Endpoint implements AutoCloseable {

        final Connection nc;
        final String endpointId;
        final Dispatcher dispatcher;

        public Endpoint(int id, String type) throws IOException, InterruptedException, TimeoutException {
            this.endpointId = type + id;
            nc = Nats.connect(SERVER_URL);
            dispatcher = nc.createDispatcher(this::handle);
            dispatcher.subscribe(endpointId + ".>");
            nc.flush(Duration.ofSeconds(1));
            System.out.println("Worker " + endpointId + " started.");
        }

        private void handle(Message msg) throws InterruptedException {
            String cmd = msg.getSubject().split("\\.")[1];
            String transactionId = msg.getHeaders().get("transactionId").get(0);
            String aId = msg.getHeaders().get("aId").get(0);
            String bId = msg.getHeaders().get("bId").get(0);
            System.out.println("Worker " + endpointId + " received a message on subject: " + msg.getSubject() + " command is: " + cmd);
            command(cmd, msg, transactionId, aId, bId);
        }

        protected abstract void command(String cmd, Message msg, String transactionId, String aId, String bId);

        @Override
        public void close() throws Exception {
            if (nc != null) {
                nc.close();
            }
        }
    }

    static class Input {
        int aId = -1;
        int bId = -1;

        @Override
        public String toString() {
            return "Input{" +
                    "aId=" + aId +
                    ", bId=" + bId +
                    '}';
        }
    }

    static Input gatherInput() {
        Input i = new Input();
        Scanner in = new Scanner(System.in);
        while (i.aId < 1 || i.aId > 2) {
            System.out.print("What Worker Type A 1 or 2? ");
            i.aId = in.nextInt();
        }
        System.out.println("Using Worker A" + i.aId);

        while (i.bId < 1 || i.bId > 3) {
            System.out.print("What Worker Type B 1, 2 or 3? ");
            i.bId = in.nextInt();
        }
        System.out.println("Using Worker B" + i.bId);
        return i;
    }}
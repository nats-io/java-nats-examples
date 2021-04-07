package io.nats.examples;

import io.nats.client.Message;
import io.nats.client.impl.NatsMessage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class PublishStyleWorkers {

    static class WorkerA extends Endpoint {
        public WorkerA(int id) throws IOException, InterruptedException, TimeoutException {
            super(id, "A");
        }

        @Override
        protected void command(String cmd, Message msg, String transactionId, String aId, String bId) throws InterruptedException {
            if (cmd.equals("step1")) {
                System.out.println("Worker " + endpointId + " step 1 processing transaction " + transactionId + ". Publishing step 1 to B" + bId);
                NatsMessage message = NatsMessage.builder().subject("B" + bId + ".step1").headers(msg.getHeaders()).build();
                nc.publish(message);
            }
            else if (cmd.equals("step2")) {
                System.out.println("Worker " + endpointId + " step 2 processing transaction " + transactionId + ". Publishing step 2 to B" + bId);
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
        protected void command(String cmd, Message msg, String transactionId, String aId, String bId) throws InterruptedException {
            if (cmd.equals("step1")) {
                System.out.println("Worker " + endpointId + " step 1 processing transaction " + transactionId + ". Publishing step 2 to A" + aId);
                NatsMessage message = NatsMessage.builder().subject("A" + aId + ".step2").headers(msg.getHeaders()).build();
                nc.publish(message);
            }
            else if (cmd.equals("step2")) {
                System.out.println("Worker " + endpointId + " step 2 transaction completed.\n");
            }
        }
    }

}
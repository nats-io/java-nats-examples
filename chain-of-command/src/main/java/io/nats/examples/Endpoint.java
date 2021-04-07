package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public abstract class Endpoint implements AutoCloseable {

    final Connection nc;
    final String endpointId;
    final Dispatcher dispatcher;

    public Endpoint(int id, String type) throws IOException, InterruptedException, TimeoutException {
        this.endpointId = type + id;
        nc = Nats.connect(App.SERVER_URL);
        dispatcher = nc.createDispatcher(this::handle);
        dispatcher.subscribe(endpointId + ".>");
        nc.flush(Duration.ofSeconds(1));
    }

    private void handle(Message msg) throws InterruptedException {
        String cmd = msg.getSubject().split("\\.")[1];
        String transactionId = msg.getHeaders().get("transactionId").get(0);
        String aId = msg.getHeaders().get("aId").get(0);
        String bId = msg.getHeaders().get("bId").get(0);
        System.out.println("Worker " + endpointId + " received a message on subject: " + msg.getSubject() + " command is: " + cmd);
        command(cmd, msg, transactionId, aId, bId);
    }

    protected abstract void command(String cmd, Message msg, String transactionId, String aId, String bId) throws InterruptedException;

    @Override
    public void close() throws Exception {
        if (nc != null) {
            nc.close();
        }
    }
}

package io.nats.maven;

import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.IOException;

public class App
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            System.out.println("Client: " + Nats.CLIENT_VERSION);
            System.out.println("Server: " + nc.getServerInfo().getVersion());
        }
        catch (InterruptedException e) {
        }
        catch (IOException e) {
        }
    }
}

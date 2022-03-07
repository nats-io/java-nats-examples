package io.nats.jsmulti;

import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.IOException;

public class App
{
    public static void main( String[] args )
    {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
        }
        catch (InterruptedException e) {
        }
        catch (IOException e) {
        }
    }
}

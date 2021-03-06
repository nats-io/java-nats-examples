package io.nats.examples;

import io.nats.client.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ConnectNKey {

    public static void main(String[] args) {
        try {
            // [begin connect_nkey]
            NKey theNKey = NKey.createUser(null); // really should load from somewhere
            Options options = new Options.Builder().
                        server("nats://localhost:4222").
                        authHandler(new AuthHandler(){
                            public char[] getID() {
                                try {
                                    return theNKey.getPublicKey();
                                } catch (GeneralSecurityException|IOException|NullPointerException ex) {
                                    return null;
                                }
                            }

                            public byte[] sign(byte[] nonce) {
                                try {
                                    return theNKey.sign(nonce);
                                } catch (GeneralSecurityException|IOException|NullPointerException ex) {
                                    return null;
                                }
                            }

                            public char[] getJWT() {
                                return null;
                            }
                        }).
                        build();
            Connection nc = Nats.connect(options);

            // Do something with the connection

            nc.close();
            // [end connect_nkey]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
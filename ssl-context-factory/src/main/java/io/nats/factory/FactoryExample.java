// Copyright 2015-2018 The NATS Authors
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

package io.nats.factory;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class FactoryExample {
    public static final String SERVER_URL = "tls://localhost:4443";

    public static final String KEYSTORE_PATH = "<path-to>/keystore.jks";
    public static final String TRUSTSTORE_PATH = "<path-to>/truststore.jks";
    public static final String TLS_ALGORITHM = "SunX509";

    public static String PASSWORD = "password";

    public static void main(String[] args) {
        factoryUsesPropertiesFromConnectionOptions();

        System.out.println();

        factoryUsesPropertiesFromEnvironment();
    }

    private static void factoryUsesPropertiesFromConnectionOptions() {
        Options options = new Options.Builder()
            .server(SERVER_URL)
            .keystorePath(KEYSTORE_PATH)
            .keystorePassword(PASSWORD.toCharArray())
            .truststorePath(TRUSTSTORE_PATH)
            .truststorePassword(PASSWORD.toCharArray())
            .tlsAlgorithm(TLS_ALGORITHM)
            .sslContextFactory(new FactoryUsesPropertiesFromConnectionOptions())
            .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected using FactoryUsesPropertiesFromConnectionOptions");
        }
        catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    private static void factoryUsesPropertiesFromEnvironment() {
        // THIS IS SIMULATING WHAT WOULD ACTUALLY BE DONE IN THE ENVIRONMENT
        System.setProperty(FactoryUsesPropertiesFromSystemProperties.PROPERTY_NATS_TLS_KEY_STORE,            KEYSTORE_PATH);
        System.setProperty(FactoryUsesPropertiesFromSystemProperties.PROPERTY_NATS_TLS_KEY_STORE_PASSWORD,   PASSWORD);
        System.setProperty(FactoryUsesPropertiesFromSystemProperties.PROPERTY_NATS_TLS_TRUST_STORE,          TRUSTSTORE_PATH);
        System.setProperty(FactoryUsesPropertiesFromSystemProperties.PROPERTY_NATS_TLS_TRUST_STORE_PASSWORD, PASSWORD);
        System.setProperty(FactoryUsesPropertiesFromSystemProperties.PROPERTY_NATS_TLS_ALGO,                 TLS_ALGORITHM);

        Options options = new Options.Builder()
            .server(SERVER_URL)
            .sslContextFactory(new FactoryUsesPropertiesFromSystemProperties())
            .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected using FactoryUsesPropertiesFromSystemProperties");
        }
        catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
}

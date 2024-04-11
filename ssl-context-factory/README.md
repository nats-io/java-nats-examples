![NATS](../images/large-logo.png)

# SSLContextFactory Example

This example demonstrates how to implement the SSLContextFactory, 
an alternative way to provide an SSL Context to the Connection Options.

### SSLContextFactory interface

The `SSLContextFactory` interface contains one method

```java
public interface SSLContextFactory {
    SSLContext createSSLContext(SSLContextFactoryProperties properties);
}
```

Here is an excerpt from `SSLContextFactoryProperties`

```java
public class SSLContextFactoryProperties {
    public final String keystorePath;
    public final char[] keystorePassword;
    public final String truststorePath;
    public final char[] truststorePassword;
    public final String tlsAlgorithm;

    public String getKeystorePath() {
        return keystorePath;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public char[] getTruststorePassword() {
        return truststorePassword;
    }

    public String getTlsAlgorithm() {
        return tlsAlgorithm;
    }
    
    ...
}
```

These properties are populated from the information used to create the connection `Options` class.
The class is designed so the values can be directly accessed via their public final fields or public getters, based on your personal style of coding.

```java
Options options = new Options.Builder()
    .server(SERVER_URL)
    .keystorePath("path/to/keystore")
    .keystorePassword("keystore-password".toCharArray())
    .truststorePath("path/to/truststore")
    .truststorePassword("truststore-password".toCharArray())
    .tlsAlgorithm("SunX509") // SunX509 is the default if this is not provided 
    .sslContextFactory(new MySSLContextFactory())
    ...
    .build();
```

There is no requirement that your factory use these properties, but since they exist they were passed on.
You could get them from the environment or maybe get them from a vault.

### Example Factories

The project provides 2 different example factories.

- The class `FactoryUsesPropertiesFromConnectionOptions` uses the instance of `SSLContextFactoryProperties` that is passed to the factory.

- The class `FactoryUsesPropertiesFromSystemProperties` gets those same values via `System.getProperty(String key)`

Some other ways to get those values:

- Get properties directly from the runtime environment using `System.getenv(String name)`

- Get properties directly from something like a vault.

### Running the Example
Running this example requires some setup.
There are six files located in the `ssl-files` directory.

- `server.conf`
- `ca.pem`
- `key.pem`
- `server.pem`
- `keystore.jks`
- `truststore.jks`

You should determine the location of the `ssl-files` folder on your machine and replace all the `<path-to>` placeholders. Fix those.

1. In the `server.conf` file, there are 3 placeholders under the tls section. See `cert_file`, `key_file` and `ca_file`.

2. In the `FactoryExample` source code there are 2 placeholders found in the string values for `KEYSTORE_PATH` and `TRUSTSTORE_PATH`.

You can then run the nats-server with the config file i.e. `nats-server -c <path-to>/server.conf`


### Example output

Here is a sample run output:

```text
Calling FactoryUsesPropertiesFromConnectionOptions.createSSLContext(...)
  These properties are passed in from the Options being used to create the connection:
    keystorePath:       ~/dev/java-nats-examples/ssl-context-factory/ssl-files/keystore.jks
    keystorePassword:   password
    truststorePath:     ~/dev/java-nats-examples/ssl-context-factory/ssl-files/truststore.jks
    truststorePassword: password
    tlsAlgorithm:       SunX509
Connected using FactoryUsesPropertiesFromConnectionOptions

Calling FactoryUsesPropertiesFromSystemProperties.createSSLContext(...)
  These properties are read from the system.
    keystorePath:       ~/dev/java-nats-examples/ssl-context-factory/ssl-files/keystore.jks
    keystorePassword:   password
    truststorePath:     ~/dev/java-nats-examples/ssl-context-factory/ssl-files/truststore.jks
    truststorePassword: password
    tlsAlgorithm:       SunX509
Connected using FactoryUsesPropertiesFromSystemProperties
```
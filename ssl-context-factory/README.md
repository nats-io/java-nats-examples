![NATS](../images/large-logo.png)

# SSLContextFactory Example

Running this example requires some setup.
There are several files located in the `ssl-files` directory.

```text
server.conf
ca.pem
key.pem
server.pem
keystore.jks
truststore.jks
```


Determine the location of the `ssl-files` folder on your machine and replace all the `<path-to>` placeholders:

1\. In the `server.conf` file, there are 3 placeholders under the tls section. See `cert_file`, `key_file` and `ca_file`.

2\. In the `FactoryExample.java` source code there are 2 placeholder, in the values for `KEYSTORE_PATH` and the `TRUSTSTORE_PATH`.

3\. Run the nats-server with the config file i.e. `nats-server -c <path-to>/server.conf`

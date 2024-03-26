![NATS](../images/large-logo.png)

# Auth Callout

An example that demonstrates basic Auth Callout handling. Run the main from the AuthCalloutServiceExample class.

## Example Notes

1. You must start the NATS server using the auth-callout-no-encrypt.conf configuration file found in the example-server-configs directory.
   For example:

    ```shell
    nats-server -c /<nats-stuff>/java-nats-examples/example-server-configs/auth-callout-no-encrypt.conf
    ```

1. The Java NKeys and JWT libraries do not yet support encrypted auth callout.

## Library Dependencies

1. The example depends on the [jwt.java](https://github.com/nats-io/jwt.java) library. 
That library has been extracted from the JNATS client and reorganized to avoid clashing with the original which has been left in the client.
It started out identical but was improved and expanded to support Auth Callout.

2. The [jwt.java](https://github.com/nats-io/jwt.java) library depends on two other libraries that have been
extracted from the JNATS client, they both also have been reorganized to avoid clashing with the original, but otherwise are essentially the same.
The libraries are [json.java](https://github.com/nats-io/json.java) and  [nkeys.java](https://github.com/nats-io/nkeys.java)
The dependency tree looks like this:

   ```text
   +--- io.nats:jwt-java:2.0.0
        +--- io.nats:nkeys-java:2.0.1
        \--- io.nats:jnats-json:2.0.0
   ```

## License

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Unless otherwise noted, the NATS source files are distributed under the Apache Version 2.0 license found in the LICENSE file.




https://github.com/nats-io/json.java
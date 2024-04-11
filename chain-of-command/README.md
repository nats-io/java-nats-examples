![NATS](../images/large-logo.png)

# Chain Of Command

The chain of command example shows subscribing with wildcard subjects to form a chain of command.
Both "publish style" and "request style" workflow are demonstrated.

The "publish style" does not know if messages were received.
The "request style" knows if the request was received, so it could handle the case when it is not.

## License

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Unless otherwise noted, the NATS source files are distributed under the Apache Version 2.0 license found in the LICENSE file.

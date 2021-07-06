![NATS](../images/large-logo.png)

# OCSP Stapling SSLContext

OCSP Stapling has been supported in Java since JDK 8. There is an excellent document from Oracle which describes it in detail.

[Client-Driven OCSP and OCSP Stapling](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/ocsp.html)

### Examples

Example of creating the SSLContext can be found in the [OCSP Example Class](src/main/java/io/nats/ocsp/OcspExample.java)
These are the methods of interest...

| Description | Method |
| --- | --- |
| _Standard TLS_ | `createStandardContext()` |
| _Vm Wide Check Revocation_ | `createVmWideOcspCheckRevocationContext()` |
| _Vm Wide Don't Check Revocation_ | `createVmWideOcspDontCheckRevocationContext()` |
| _Siloed Check Revocation_ | `createSiloedContextCheckRevocation()` |

### Entire VM Approach

It's actually trivial to turn on Client Side OCSP revocation checking. It's as simple at this code, adding system properties:

```java
System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
System.setProperty("com.sun.net.ssl.checkRevocation", "true");
```

The caveat here is that these properties apply to every single SSLContext running in that VM. 
These properties are checked at runtime, each time a TLS handshake is made, so it cannot be turned on
to create an `SSLContext` then turned off. If it is turned off, revocation checking will not happen.

### Siloed Approach

If it's that easy to turn on OCSP stapling with revocation, why do we need the example?

Consider 3 different types of connections.

1. Standard TLS
2. OCSP with revocation checking
3. OCSP without revocation checking

It appears that setting the system properties does not affect Standard TLS certificate handshakes. 
But if you need both OCSP with revocation checking and OCSP without, you cannot use the `com.sun.net.ssl.checkRevocation` property,
so must use the siloed implementation for the context that will check revocation.
Find your configuration in this table to see if you have to set the system properties / which OCSP context implementation to use.

| Have Standard TLS? | Have  OCSP With Revocation? | Have OCSP Without Revocation? | Enable Status Request Extension Flag? | Check Revocation Flag? | Use Context Implementations | 
| --- | --- | --- | --- | --- | --- |
| Yes | No  | No  | false | false | _Standard TLS_ | 
| Yes | Yes | No  | true  | true  | _Standard TLS_ and _Vm Wide Check Revocation_ |
| Yes | No  | Yes | true  | false | _Standard TLS_ and _Vm Wide Don't Check Revocation_ |
| Yes | Yes | Yes | true  | false | _Standard TLS_, _Siloed Check Revocation_ and _Vm Wide Don't Check Revocation_ |
| No  | No  | No  | false | false | None |
| No  | Yes | No  | true  | true  | _Vm Wide Check Revocation_ |
| No  | No  | Yes | true  | false | _Vm Wide Don't Check Revocation_ |
| No  | Yes | Yes | true  | false | _Siloed Check Revocation_ and _Vm Wide Don't Check Revocation_ |

Please be aware. There are a few examples on the internet that guided development examples, in fact the siloed version is almost identical to those,
but none of them set the `enableStatusRequestExtension` flag. As far as we can tell it simply does not work without setting the flag!

## Licenses

This example leverages the [SSLContext Kickstart project](https://github.com/Hakky54/sslcontext-kickstart)
which operates under the [Apache License 2.0](https://github.com/Hakky54/sslcontext-kickstart/blob/master/LICENSE)
Their library had some functionality scoped `private` which we needed to use directly so we copied it and made it public.
This usage is allowed by Apache 2.0 license policy, but we are noting it here for full transparency.

That library and this example both use [Bouncy Castle](https://www.bouncycastle.org/) which has this [Licence](https://www.bouncycastle.org/license.html), an [MIT License](https://opensource.org/licenses/MIT)

Unless otherwise noted, NATS source files are distributed under [Apache Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

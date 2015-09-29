CAPS
====

MITM Proxy Server for SSL and client certificate injection that can be used for testing with Internet Explorer.
This avoids using slow UI automation to select the certificate from the native popup when running tests with selenium and InternetExplorerDriver.

This code is only a proof of concept to my idea on how to speed up selenium tests using IEDriver with targets that require client certificates.
Lot of time gets wasted trough native automation of the dialogs while selecting a client certificate, especially if there are 20+ different certificates available.

```
<dependency>
  <groupId>ch.racic</groupId>
  <artifactId>caps</artifactId>
  <version>2.0.2</version>
</dependency>
```

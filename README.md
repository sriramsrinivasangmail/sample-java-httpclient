Sample java http client



 example to  get a client to trust a self-signed certificate the cacert "internal-nginx.pem"  directly into the JDK's keystore.

The sample self-signed certificate is here ssl-config/internal-nginx.pem 

and the sample openjdk cacert is also under ssl-config/

---

```

export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk

openssl x509 -in internal-nginx.pem -inform pem -out /tmp/local-ca.der -outform der

keytool -v -printcert -file /tmp/local-ca.der

keytool -importcert -alias local-CA -keystore $JAVA_HOME/jre/lib/security/cacerts -file /tmp/local-ca.der

```


---

Sample Test program run:

java HTTPClientTest https://internal-nginx-svc.ibm-private-cloud.svc.cluster.local/diag

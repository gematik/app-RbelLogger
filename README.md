# ![Logo](./doc/images/RBelLogger-320.png)Using the RbelLogger Application

read from pcap file and render to html

```
java -jar target/rbellogger-1.0-SNAPSHOT-jar-with-dependencies.jar -pcap src/test/resources/ssoTokenFlow.pcap -dump -html
```

read from pcap file and render to html using IDP value shader config

```
java -jar target/rbellogger-1.0-SNAPSHOT-jar-with-dependencies.jar -pcap src/test/resources/ssoTokenFlow.pcap -dump -html -shade-values idpShadeStrings.properties 
```

### Caveats

Sometimes the nPCAP/Wireshark do not find any network interfaces. In that case see
https://www.outlookappins.com/windows-10/wireshark-no-interfaces-found/
Perform the command prompt solution or run wireshark once as admin.
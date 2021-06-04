# ![Logo](./doc/images/RBelLogger-320.png)
# RBeL Logger

# Content

*  [Introduction] (#Introduction)
*  [Traffic Sources] (#Traffic Sources)
    * [PCaP-Capture] (#PCaP-Capture)
    * [Wiremock-Capture] (#Wiremock-Capture)
*  [RBeL-Path] (#RBeL-Path)
    * [JEXL expressions] (#JEXL expressions)
* [HTML-Rendering] (#HTML-Rendering)
    * [Notes] (#Notes)
    * [Value-Shading] (#Value-Shading)
* [Using the RBeL Application] (#Using the RBeL Application)
    * [Caveats] (#Caveats)


## Introduction

This tools allows the capture, analysis and rendering of HTTP-Traffic. It can
* capture traffic from multiple sources (PCAP, Wiremock, Mock-Server, Custom Sources)
* break up nested, layered protocols (XML, JSON, JWT, JWE, ASN.1, VAU-Protocol)
* decipher content and verify signatures (Keys can be added on-the-fly or read from file)
* search in the layered structure via RBeL-Path (see below, XPath-Style query expressions)
* render descriptive HTML-renderings of the complete flow

## Traffic Sources

### PCaP-Capture

The following code block reads from a pcap-File
```java
final PCapCapture pCapCapture = PCapCapture.builder()
.pcapFile("src/test/resources/deregisterPairing.pcap")
// to read live from a device exchange the previous line with
// .deviceName("lo0")
.build();

final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
.addCapturer(pCapCapture)
);

pCapCapture.close();

// Note: since this is an offline capture close works interchangeably with 
// pCapCapture.initialize();

// If you want to read from a real device then you must first call
// .initialize() and .close() to finalize the capture.
```

### Wiremock-Capture

Wiremock provides a very basic, but configureable proxy-server. The WiremockCapture uses such
a server to record traffic:

```java
final WiremockCapture wiremockCapture = WiremockCapture.builder()
    .proxyFor(MOCK_SERVER_ADDRESS)
    .build();

final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
    .addCapturer(wiremockCapture));

// initialize the capture (can also be performed prior to the RBeL-Logger instantiation)
wiremockCapture.initialize();

// perform a sample request
Unirest.post(wiremockCapture.getProxyAdress() + "/foobar").asString();

// perform assertions on the captured traffic
final RbelHttpRequest request = (RbelHttpRequest) rbelLogger.getMessageHistory().get(0);
assertThat(request)
    .hasFieldOrPropertyWithValue("method", "POST");
assertThat(request.getPath().getOriginalUrl())
    .isEqualTo(MOCK_SERVER_ADDRESS + "/foobar");
```
## RBeL-Path
RBeL-Path is a XPath or JSON-Path inspired expression-language enabling the quick traversal 
of captured RBeL-Traffic.

A simple example:
```java
assertThat(convertedMessage.findRbelPathMembers("$.header"))
    .containsExactly(convertedMessage.getHeader());
```
RBeL-Path provides seamless retrieval of nested members. 

The following message contains a JWT (Json Web Token, a structure which contains of a header, a body and a signature).
In the body there is a claim (essentially a Key/Value pair represented in a JSON-structure)
named `nbf` which we want to inspect. 

Please note that the RBeL-Path expression contains no information about the types in 
the structure. This expression would also work if the HTTP-message contained a JSON-Object 
with the corresponding path, or an XML-Document.
```java
assertThat(convertedMessage.findRbelPathMembers("$.body.body.nbf"))
    .containsExactly(convertedMessage.getFirst("body").get().
        getFirst("body").get().
        getFirst("nbf").get());
```

You can also use wildcards to retrieve all members of a certain level:

```
$.body.[*].nbf
```

Alternatively you can recursively descend and retrieve all members:

```
$..nbf
```
and 
```
$.body..nbf
```
will both return the same elements (maybe amongst other elements).

### JEXL expressions
RBeL-Path can be integrated with JEXL-expression, giving a much more powerful and flexible 
tool to extract certain element. This can be done using the syntax from the following example:
```
$..[?(key=='nbf')]
```
The expression in the round-brackets is interpreted as JEXL. The available syntax is 
described in more detail here: https://commons.apache.org/proper/commons-jexl/reference/syntax.html

The variables that can be used are listed below:

* `element` contains the current RBeL-Element
* `parent` gives direct access to the parent element of the current element. 
  Is `null` if not present
* `message` contains the HTTP-Message under which this element was found
* `request` is the corresponding HTTP-Request. If `message` is a response, then the corresponding Request will be returned. 
If `message` is a request, then `message` will be returned.
* `key` is a string containing the key that the current element can be found under in the parent-element.
* `path` contains the complete sequence of keys from `message` to `element`.
* `type` is a string containing the class-name of `element` (eg `RbelJsonElement`).
* `content` is a string describing the content of `element`. The actual representation 
dependens heavily on the type of `element`.

## HTML-Rendering

One of the main features of the RBeL-Logger is the ability to render a captured message flow
into a HTML-representation.

```java
final String html = RbelHtmlRenderer.render(rbelLogger.getMessageHistory());
FileUtils.writeStringToFile(new File("out.html"), html);
```

The HTML-rendering showcases the nested structure of the communication as captured and 
analyzed by the RBeL-Logger.

### Notes

RBeL contains some tools to make the resulting HTML-Rendering more expressive.

Notes can be added manually
```java
RbelHttpResponse convertedMessage = (RbelHttpResponse) RbelLogger.build()
    .getRbelConverter().convertMessage(curlMessage);
convertedMessage.setNote("Note to be displayed on the resulting HTML");
```

or via JEXL-Expressions:
````java
RbelLogger rbelLogger = RbelLogger.build();
    rbelLogger.getValueShader().addJexlNoteCriterion("key == 'Version'", "Extra note");
RbelHttpResponse convertedMessage = (RbelHttpResponse) rbelLogger.getRbelConverter()
    .convertMessage(curlMessage);

assertThat(convertedMessage.getHeader().getFirst("Version").get().getNote())
    .isEqualTo("Extra note");
````

Note that the example above is stylistically bad: In any reasonable message-flow there will
be multiple entries with the key 'Version'. Try to be as concise as possible with your 
JEXL-expression:

```
request.url =~ '.*/sign_response.*'
 && request.method=='GET' 
 && message.isResponse 
 && path == 'body' 
 && type == 'RbelJsonElement'
```

### Value-Shading

Similarly, you might want to shade certain value, ie replace their values with something else
to increase readability. 
```java
rbelLogger.getValueShader().addJexlShadingCriterion("key == 'Version'", "<version: %s>");

rbelLogger.getValueShader().addJexlShadingCriterion("key == 'nbf' && empty(element.parentNode)", "<nbf in JWT>")
```
The second parameter is a Stringf-value that may or may not reference the original given value.

## Using the RBeL Application
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
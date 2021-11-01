# ![Logo](./doc/images/RBelLogger-320.png)
# RBeL Logger

# Content

*  [Introduction](#Introduction)
*  [Traffic Sources](#Traffic Sources)
    * [PCaP-Capture](#PCaP-Capture)
    * [Wiremock-Capture](#Wiremock-Capture)
*  [RBelElement](#RBelElement)
*  [RBeL-Path](#RBeL-Path)
    * [JEXL expressions](#JEXL expressions)
* [HTML-Rendering](#HTML-Rendering)
    * [Notes](#Notes)
    * [Value-Shading](#Value-Shading)

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
// .initialize() to start the capture 
// and .close() to finalize the capture.
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

## RBelElement
### (Structure of parsed content)

The root-class for all content is RbelElement. The RbelElement is an immutable. A RbelElement contains
* the raw content which the element contains (byte[])
* the parent node of itself (is null if the element is the root element)
* a note-reference (can be empty). This can be explanatory text which will be rendered in the HTML
* a list of facets. Facets are ways to interpret the content (read on for an explanation)

Every time an element is parsed it is presented to a list of Converters. These converters may 
add facets to the element, but can not alter the data that is present (rawData and parent).
To be able to add child-nodes the child nodes are bundled in the facets (which are stored in 
a list in the RbelElement). You can always add to this list, thus adding facets (which 
represents a possible interpretation of the data of the Element).

If we abstract away from the facets we arrive at a tree-structure, with the HTTP-Message 
being the root node. Since every node (every RbelElement) has its raw-data this data is 
duplicated multiple times in-memory. The parsed Rbel-structure is thus not an efficient way
of storing data (a thing that should be remembered when working with very large messages).

## RBeL-Path
RBeL-Path is a XPath or JSON-Path inspired expression-language enabling the quick traversal 
of captured RBeL-Traffic (navigation of the RbelElement-tree).

A simple example:
```java
assertThat(convertedMessage.findRbelPathMembers("$.header"))
    .containsExactly(convertedMessage.getFacetOrFail(RbelHttpMessageFacet.class).getHeader());
```
or
```java
assertThat(convertedMessage.findElement("$.header"))
    .get()
    .isSameAs(convertedMessage.getFacetOrFail(RbelHttpMessageFacet.class).getHeader());
```
(The first example executes the RbelPath and returns a list of all matching element, the 
second one returns an Optional containing a single result. If there are multiple matches an
exception is given.)

RBeL-Path provides seamless retrieval of nested members. 

Here is an example of HTTP-Message containing a JSON-Body:

# ![Logo](./doc/images/rbelPath1.jpg)

The following message contains a JWT (Json Web Token, a structure which contains of a header, a body and a signature).
In the body there is a claim (essentially a Key/Value pair represented in a JSON-structure)
named `nbf` which we want to inspect. 

Please note that the RBeL-Path expression contains no information about the types in 
the structure. This expression would also work if the HTTP-message contained a JSON-Object 
with the corresponding path, or an XML-Document.
```java
assertThat(convertedMessage.findRbelPathMembers("$.body.body.nbf"))
    .containsExactly(convertedMessage.getFirst("body").get()
    .getFirst("body").get()
    .getFirst("nbf").get()
    .getFirst("content").get());
```
(The closing .getFirst("content") in the assertion is due to a fix to make RbelPath 
in JSON-Context easier: If the RbelPath ends on a JSON-Value-Node the corresponding content is returned.)
# ![Logo](./doc/images/rbelPath2.jpg)

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

### Debugging Rbel-Expressions

To help users create RbelPath-Expressions there is a Debug-Functionality which produces 
log message designed to help. These can be activated by `RbelOptions.activateRbelPathDebugging();`.
Please note that this is strictly intended for development purposes and will flood the log with quite 
a lot of messages. Act accordingly!

To get a better feel for a RbelElement (whether it being a complete message or just a part) you can print
the tree with the `RbelElementTreePrinter`. It brings various options:
```java
RbelElementTreePrinter.builder()
    .rootElement(this) //the target element
    .printKeys(printKeys) // should the keys for every leaf be printed?
    .maximumLevels(100) // only descend this far into the three
    .printContent(true) // should the content of each element be printed?
    .build()
    .execute();
```

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

Sometimes the nPCAP/Wireshark does not find any network interfaces. In that case see
https://www.outlookappins.com/windows-10/wireshark-no-interfaces-found/
Perform the command prompt solution or run wireshark once as admin.
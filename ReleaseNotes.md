# Changelog RBeL Logger

# Release 0.27.2

## Features

* TGR-553: Return parsed traffic in RbelFileWriterUtils

# Release 0.27.1

## Bugfixes

* TGR-530: Improved resilience when parsing Rbel-Files

# Release 0.27.0

## Bugfixes

* RBEL-63: Improved resilience when parsing URIs

# Release 0.26.2

## Bugfixes

* TGR-523: Fix for Regex-relevant characters in MTOM-Boundaries

## Features

* TGR-523: UUID can be supplied for RbelElements

# Release 0.26.1

## Features

* TGR-521: Explicitly set message-pairing is now conserved

# Release 0.26.0

## Features

* RBEL-61: Binary-only messages can now be rendered correctly
* TGR-498: Modifications should contain a TTL 
* TGR-404: Sicct-Envelope can be parsed
* RBEL-62: The information of X509-Certificates now shows up in the element tree
* RBEL-62: JEXL-expressions now support the '@'-operator
* RBEL-22: Basic-Support for SICCT messages added

# Release 0.24.0

## Bugfixes

* RBEL-59: BearerToken are now output to HTML-Renderings

# Release 0.23.2

## Bugfixes

* RBEL-58: printTreeStructure now correctly displays root-node key and content for singular structures

# Release 0.23.2

## Bugfixes

* RBEL-53: RbelMultiMap now correctly shields the internal organisation of element ordering.
* RBEL-54: Timing-Facet added.

# Release 0.23.1

## Changed

* RBEL-49: Defunct HTTP-charset will now be guessed and/or ignored
* RBEL-49: MTOM-parsing now also works on \\n-only messages (instead of \\r\\n)
* PKITS-143: Rbel can now modify JWT-Signatures

# Release 0.23.0

## Bugfixes

* RBEL-48: Multiple JKS-Elements can now be parsed correctly

# Release 0.22.3

## Changed

* TGR-389: Derived VAU-Keys now show the name of the originating parent-key
* TGR-358: Multiple facets can now be rendered in the same element
* RBEL-47: HTML-Documents can now be parsed even if not correct XML
* TGR-418: Rbel now manages its own buffer (if activated)
* TGR-419: Cleaned up API of RbelJexlExecutor

# Release 0.22.1

## Changed

* RBEL-41: removed too restrictive URL-validation

# Release 0.22.0

## Changed

* TGR-264: add support for reason phrases in RBEL
* RBEL-43: XML-Files encoded as Base64 are now parsed
* RBEL-41: Removed strict RbelHostname validations

# Release 0.21.0

## Changed

* TGR-37: Multiple notes are now supported on a single element
* TGR-37: Exception in converters are now caught, logged and added as an error-note
* RBEL-25: Charset-Support added. XML- and HTTP-Content are now parsed with correct charset.

## Bugfix

* RBEL-37: Long adjacent texts in XMLs are now merged

# Release 0.20.0

## Changed 

* RBEL-34: removed clojars repo from pom and moved it to settings.xml
* TGR-167: Modification extended for Vau Messages (both ERP and EPA)

## Bugfix

* TGR-167: Modifications now also work with chunked-encoded messages

# Release 0.19.0

## Changed

* changed clojars repo in pom to HTTPS

# Release 0.18.0

## Bugfix

* Modifications produzieren jetzt valides JSON
* Modifications passen ggf einen vorhandenen Content-Length-Header an

## Features

* RBEL-24: JWTs neu schreiben und signieren
* TGR-136: Clustering von Requests/Responses via bundledMessages
* RBEL-27: Modifications für JWE erweitern + symmetrische Schlüssel für JWT

# Release 0.17.0
* ANSI Colors Bugfix

# Release 0.16.0
* RbelPath-Debugging added
* RbelOptions added (centralizes global configuration)
* Query-Parameters are now parsed more resilient

# Release 0.15.0
* Modifications added

# Release 0.14.0
* ASN.1 Enumerated can now be parsed. More lenient error handling added.

# Release 0.13.0
* Sequence-Numbers are now consistent

# Release 0.12.0
* Clear converters added

# Release 0.11.0
* RbelPath descends more robustly into Value-Facets
* ConcurrentModification-Bug in HTML Rendering fixed

# Release 0.10.0
* Updated Documentation
* Smaller Bugfixes
* Filewriter added

# Release 0.9.0
* Restructuring of parsing model
* Introducing Facets
* Restructuring of Renderer
* Removal of Wiremock from release (includes wiremockListener)
* Removal of CLI-Application

# Release 0.8.6
* internal fix release

# Release 0.8.5
* Binary classification of HTTP-Bodies improved
* RbelMessage as top-level object introduced
* Vau-Message handling refined

# Release 0.8.4
* ASN.1 Parsing added

# Release 0.8.3
* VAU-Protocol Decryption added (ERP and ePA)
* Binary-Data Support added

# Release 0.8.2
* Support for TCP Segmentation
* Reworked Read-In of PCAP-Files
* Support for XML-Files

# Release 0.8.1
* NPEs in MultiValueMap fixed
* RbelPath added (search for nested nodes with greater comfort)

# Release 0.8.0
* Configuration customizable

# Release 0.7.4
* Multi-Value Support
* Rework Value Structure in RbelElement
* Note-Support for JSON Elements
* Proxy-Settings for Wiremock enabled

# Release 0.7.2
* Bugfix

# Release 0.7.1
* Better key management
* Pretty printing Wiremock Requests

# Release 0.7.0
* jexl shading added
* raw content dialog added

# Release 0.6.3
* add project name to pom

# Release 0.6.2
* removed dependency jose4j

# Release 0.6.1
* change jose4j dependency

# Release 0.6.0
* Initial release

# Changelog RBeL Logger

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
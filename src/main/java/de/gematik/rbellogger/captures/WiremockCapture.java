/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.captures;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.*;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wiremock.com.google.common.net.MediaType;

@Slf4j
@Getter
@EqualsAndHashCode
public class WiremockCapture extends RbelCapturer {

    private final String proxyFor;
    private final ProxySettings proxySettings;
    private final WireMockConfiguration wireMockConfiguration;
    private WireMockServer wireMockServer;
    private boolean isInitialized;

    @Builder
    public WiremockCapture(final RbelConverter rbelConverter,
        final String proxyFor, final ProxySettings proxySettings, final WireMockConfiguration wireMockConfiguration) {
        super(rbelConverter);
        this.proxyFor = proxyFor;
        this.proxySettings = proxySettings;
        this.wireMockConfiguration = wireMockConfiguration;
    }

    public WiremockCapture initialize() {
        if (isInitialized) {
            return this;
        }

        log.info("Starting Wiremock-Capture. This will boot a proxy-server for the target url '{}'", proxyFor);
        final WireMockConfiguration wireMockConfiguration = getWireMockConfiguration();
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.any(WireMock.anyUrl())
            .willReturn(aResponse().proxiedFrom(proxyFor)));

        wireMockServer.addMockServiceRequestListener((request, response) -> {
            getRbelConverter().convertMessage(requestToRbelMessage(request));
            getRbelConverter().convertMessage(responseToRbelMessage(response));
        });

        log.info("Started Wiremock-Server at '{}'.", wireMockServer.baseUrl());

        isInitialized = true;

        return this;
    }

    private WireMockConfiguration getWireMockConfiguration() {
        if (this.wireMockConfiguration != null) {
            return this.wireMockConfiguration;
        }
        final WireMockConfiguration wireMockConfiguration = WireMockConfiguration.options()
            .dynamicPort()
            .trustAllProxyTargets(true)
            .enableBrowserProxying(false);
        if (proxySettings != null) {
            wireMockConfiguration.proxyVia(proxySettings);
        }
        return wireMockConfiguration;
    }

    private RbelElement requestToRbelMessage(final Request request) {
        return RbelHttpRequest.builder()
            .method(request.getMethod().getName())
            .path(getRequestUrl(request))
            .header(mapHeader(request.getHeaders()))
            .body(getRbelConverter().convertMessage(
                convertMessageBody(request.getBodyAsString(), request.getHeaders().getContentTypeHeader())))
            .build()
            .setRawMessage(request.getMethod().toString() + " " + request.getUrl() + " HTTP/1.1\n"
                + request.getHeaders().all().stream().map(HttpHeader::toString)
                .collect(Collectors.joining("\n")) + "\n\n"
                + request.getBodyAsString());
    }

    private RbelPathElement getRequestUrl(Request request) {
        final RbelElement pathElement = getRbelConverter()
            .convertMessage((proxyFor == null ? "" : proxyFor)
                + request.getUrl());
        if (pathElement instanceof RbelPathElement) {
            return (RbelPathElement) pathElement;
        } else {
            throw new RuntimeException("Non-matching URL-component: " + pathElement.getContent());
        }
    }

    private RbelElement responseToRbelMessage(final Response response) {
        return RbelHttpResponse.builder()
            .responseCode(response.getStatus())
            .header(mapHeader(response.getHeaders()))
            .body(getRbelConverter().convertMessage(
                convertMessageBody(response.getBodyAsString(), response.getHeaders().getContentTypeHeader())))
            .build()
            .setRawMessage("HTTP/1.1 " + response.getStatus() + " "
                + (response.getStatusMessage() != null ? response.getStatusMessage() : "") + "\n"
                + response.getHeaders().all().stream().map(HttpHeader::toString)
                .collect(Collectors.joining("\n"))
                + "\n\n" + response.getBodyAsString());
    }

    private RbelElement convertMessageBody(String bodyAsString, ContentTypeHeader contentTypeHeader) {
        if (Optional.ofNullable(contentTypeHeader)
            .map(ContentTypeHeader::mimeTypePart)
            .map(mime -> mime.equals(MediaType.FORM_DATA.toString()))
            .orElse(false)) {
            try {
                return new RbelMapElement(Stream.of(bodyAsString.split("&"))
                    .map(str -> str.split("="))
                    .collect(
                        Collectors.toMap(array -> array[0], array -> getRbelConverter().convertMessage(array[1]))));
            } catch (Exception e) {
                log.warn("Unable to parse form-data '" + bodyAsString + "'. Using fallback");
                return getRbelConverter().convertMessage(bodyAsString);
            }
        } else {
            return getRbelConverter().convertMessage(bodyAsString);
        }
    }

    private RbelMultiValuedMapElement mapHeader(HttpHeaders headers) {
        final Map<String, List<RbelElement>> headersMap = headers.all().stream()
            .collect(Collectors.toMap(e -> e.key(), e -> e.values().stream()
                .map(v -> getRbelConverter().convertMessage(v))
                .collect(Collectors.toList())));
        return new RbelMultiValuedMapElement(headersMap);
    }

    public String getProxyAdress() {
        return "http://localhost:" + wireMockServer.port();
    }

    @Override
    public void close() {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }
}

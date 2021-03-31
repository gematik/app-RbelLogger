/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.captures;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.*;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.*;
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
    private WireMockServer wireMockServer;
    private boolean isInitialized;

    @Builder
    public WiremockCapture(RbelConverter rbelConverter, String proxyFor) {
        super(rbelConverter);
        this.proxyFor = proxyFor;
    }

    public WiremockCapture initialize() {
        if (isInitialized) {
            return this;
        }
        final WireMockConfiguration wireMockConfiguration = WireMockConfiguration.options()
            .dynamicPort()
            .dynamicHttpsPort()
            .trustAllProxyTargets(true)
            .enableBrowserProxying(false);
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.any(WireMock.anyUrl())
            .willReturn(aResponse().proxiedFrom(proxyFor)));

        wireMockServer.addMockServiceRequestListener((request, response) -> {
            getRbelConverter().convertMessage(requestToRbelMessage(request));
            getRbelConverter().convertMessage(responseToRbelMessage(response));
        });

        isInitialized = true;

        return this;
    }
    private RbelElement requestToRbelMessage(final Request request) {
        return RbelHttpRequest.builder()
            .method(request.getMethod().getName())
            .path((RbelPathElement) getRbelConverter().convertMessage(request.getUrl()))
            .header(mapHeader(request.getHeaders()))
            .body(getRbelConverter().convertMessage(
                convertMessageBody(request.getBodyAsString(), request.getHeaders().getContentTypeHeader())))
            .build();
    }

    private RbelElement responseToRbelMessage(final Response response) {
        return RbelHttpResponse.builder()
            .responseCode(response.getStatus())
            .header(mapHeader(response.getHeaders()))
            .body(getRbelConverter().convertMessage(
                convertMessageBody(response.getBodyAsString(), response.getHeaders().getContentTypeHeader())))
            .build();
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

    private RbelMapElement mapHeader(HttpHeaders headers) {
        final Map<String, RbelElement> headersMap = headers.all().stream()
            .collect(Collectors
                .toMap(HttpHeader::key, httpHeader -> getRbelConverter().convertMessage(httpHeader.firstValue())));
        return new RbelMapElement(headersMap);
    }

    public String getProxyAdress() {
        return "http://localhost:" + wireMockServer.port();
    }

    public String getTlsProxyAdress() {
        return "https://localhost:" + wireMockServer.httpsPort();
    }

    @Override
    public void close() {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }
}

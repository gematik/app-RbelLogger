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

package de.gematik.rbellogger.apps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.WiremockCapture;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.data.RbelHttpRequest;
import de.gematik.rbellogger.data.RbelHttpResponse;
import de.gematik.rbellogger.data.RbelMapElement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WiremockCaptureTest {

    private static final String BODY = "Coole Antwort";
    private static String MOCK_SERVER_ADDRESS;
    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void startRecording() {
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.options()
            .dynamicPort()
            .dynamicHttpsPort()
            .trustAllProxyTargets(true)
            .enableBrowserProxying(false);
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();
        MOCK_SERVER_ADDRESS = "http://localhost:" + wireMockServer.port();
    }

    @AfterAll
    public static void stopRecording() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void init() {
        wireMockServer.resetScenarios();
    }

    @Test
    public void simpleExchange_shouldLog() throws UnirestException {
        final WiremockCapture wiremockCapture = WiremockCapture.builder()
            .proxyFor(MOCK_SERVER_ADDRESS)
            .build()
            .initialize();

        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addCapturer(wiremockCapture));

        wireMockServer.stubFor(post(urlEqualTo("/foobar"))
            .willReturn(aResponse()
                .withStatus(666)
                .withBody(BODY)));

        Unirest.post(wiremockCapture.getProxyAdress() + "/foobar")
            .field("foo", "bar")
            .asString();

        final RbelHttpRequest request = (RbelHttpRequest) rbelLogger.getMessageHistory().get(0);
        assertThat(request)
            .hasFieldOrPropertyWithValue("method", "POST");
        assertThat(request.getPath().getOriginalUrl())
            .isEqualTo("/foobar");
        assertThat(request.getBody())
            .isInstanceOf(RbelMapElement.class);

        final RbelHttpResponse response = (RbelHttpResponse) rbelLogger.getMessageHistory().get(1);
        assertThat(response)
            .hasFieldOrPropertyWithValue("responseCode", 666);
        assertThat(response.getBody().getContent())
            .isEqualTo(BODY);
    }
}

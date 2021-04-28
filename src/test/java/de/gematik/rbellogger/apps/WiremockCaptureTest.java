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

package de.gematik.rbellogger.apps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import de.gematik.rbellogger.data.RbelStringElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WiremockCaptureTest {

    private static final String BODY = "Coole Antwort";
    private static String MOCK_SERVER_ADDRESS;
    private static WireMockServer serverMock;

    @BeforeAll
    public static void startRecording() {
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.options()
            .dynamicPort()
            .dynamicHttpsPort()
            .trustAllProxyTargets(true)
            .enableBrowserProxying(false);
        serverMock = new WireMockServer(wireMockConfiguration);
        serverMock.start();
        MOCK_SERVER_ADDRESS = "http://localhost:" + serverMock.port();
    }

    @AfterAll
    public static void stopRecording() {
        serverMock.stop();
    }

    @BeforeEach
    public void init() {
        serverMock.resetScenarios();
    }

    @Test
    public void simpleExchange_shouldLog() throws UnirestException, IOException {
        final WiremockCapture wiremockCapture = WiremockCapture.builder()
            .proxyFor(MOCK_SERVER_ADDRESS)
            .build()
            .initialize();

        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addCapturer(wiremockCapture));

        serverMock.stubFor(post(urlEqualTo("/foobar"))
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
            .isEqualTo(MOCK_SERVER_ADDRESS + "/foobar");
        assertThat(request.getBody())
            .isInstanceOf(RbelMapElement.class);

        final RbelHttpResponse response = (RbelHttpResponse) rbelLogger.getMessageHistory().get(1);
        assertThat(response)
            .hasFieldOrPropertyWithValue("responseCode", 666);
        assertThat(response.getBody().getContent())
            .isEqualTo(BODY);

        FileUtils.writeStringToFile(new File("target/wiremock.html"),
            new RbelHtmlRenderer()
                .doRender(rbelLogger.getMessageHistory()), Charset.defaultCharset());
    }

    @Test
    public void duplicateHeader_shouldBePresent() throws UnirestException, IOException {
        final WiremockCapture wiremockCapture = WiremockCapture.builder()
            .proxyFor(MOCK_SERVER_ADDRESS)
            .build()
            .initialize();

        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addCapturer(wiremockCapture));

        serverMock.stubFor(post(urlEqualTo("/foobar"))
            .willReturn(aResponse()
                .withStatus(666)
                .withHeader("foo", "bar1", "bar2")
                .withBody(BODY)));

        Unirest.post(wiremockCapture.getProxyAdress() + "/foobar")
            .field("foo", "bar")
            .asString();
        FileUtils.writeStringToFile(new File("target/doubleHeader.html"),
            new RbelHtmlRenderer()
                .doRender(rbelLogger.getMessageHistory()), Charset.defaultCharset());

        final RbelHttpResponse response = (RbelHttpResponse) rbelLogger.getMessageHistory().get(1);
        assertThat(response.getHeader().getChildElements())
            .contains(Pair.of("foo", new RbelStringElement("bar1")),
                Pair.of("foo", new RbelStringElement("bar2")));
    }
}

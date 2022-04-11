/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.rbellogger.converter;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelMultiMap;
import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.data.facet.RbelVauEpaFacet;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelVauKey;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class VauEpaConverterTest {

    private PCapCapture pCapCapture;
    private RbelLogger rbelLogger;

    {
        RbelHtmlRenderer.registerFacetRenderer(
            new RbelHtmlFacetRenderer() {
                @Override
                public boolean checkForRendering(RbelElement element) {
                    return element.hasFacet(TestFacet.class);
                }

                @Override
                public ContainerTag performRendering(RbelElement element, Optional<String> key, RbelHtmlRenderingToolkit renderingToolkit) {
                    return ancestorTitle().with(
                        vertParentTitle().with(
                            childBoxNotifTitle(CLS_BODY).with(t2("Test Facet"))
                                .with(div("Some Notes"))
                        )
                    );
                }

                @Override
                public int order() {
                    return 100;
                }
            }
        );
    }

    @BeforeEach
    public void setUp() {
        pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/vauFlow.pcap")
            .filter("")
            .build();
        rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addPostConversionListener((rbelElement, converter) -> {
                if (rbelElement.hasFacet(RbelVauEpaFacet.class)) {
                    rbelElement.addFacet(new TestFacet());
                }
            })
            .addCapturer(pCapCapture)
        );
        pCapCapture.close();
    }

    @SneakyThrows
    @Test
    public void shouldRenderCleanHtml() {
        FileUtils.writeStringToFile(new File("target/vauFlow.html"),
            RbelHtmlRenderer.render(rbelLogger.getMessageHistory()));
    }

    @Test
    public void shouldParseHandshakeNestedMessage() throws IOException {
        RbelLogger epa2Logger = RbelLogger.build(new RbelConfiguration()
            .setActivateAsn1Parsing(false)
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources")));

        String rawSavedVauMessages = FileUtils.readFileToString(new File("src/test/resources/vauEpa2Flow.rawHttpDump"));
        Stream.of(rawSavedVauMessages.split("\n\n"))
            .map(Base64.getDecoder()::decode)
            .forEach(msgBytes -> epa2Logger.getRbelConverter().parseMessage(msgBytes, null, null));

        assertThat(epa2Logger.getMessageHistory().get(24)
            .findRbelPathMembers("$.body.Data.content.decoded.AuthorizationAssertion.content.decoded.Assertion.Issuer.text")
            .get(0).getRawStringContent())
            .isEqualTo("https://aktor-gateway.gematik.de/authz");
    }

    @Test
    @DisplayName("VAU-Flow mit einem \\n Zeilenumbruch. \\r fehlt, trotzdem soll der Parser das MTOM parsen können")
    public void parseAnotherLogFile() throws IOException {
        RbelLogger epa2Logger = RbelLogger.build(new RbelConfiguration()
            .setActivateAsn1Parsing(false)
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources")));

        String rawSavedVauMessages = FileUtils.readFileToString(new File("src/test/resources/trafficLog.tgr"));
        Stream.of(rawSavedVauMessages.split("\n\n\n"))
            .forEach(tgrJson -> epa2Logger.getRbelConverter().parseMessage(
                Base64.getDecoder().decode(tgrJson.split("\"")[15]), null, null));

        assertThat(epa2Logger.getMessageHistory().get(9)
            .findElement("$.body.message.reconstructedMessage.Envelope.Header.Action.text").get()
            .getRawStringContent())
            .isEqualTo("urn:ihe:iti:2007:ProvideAndRegisterDocumentSet-b");
    }

    @Test
    public void nestedHandshakeMessage_ShouldParseNestedJson() {
        assertThat(rbelLogger.getMessageHistory())
            .hasSize(8);

        assertThat(rbelLogger.getMessageHistory().get(0).findRbelPathMembers("$.body.Data.content.decoded.DataType.content")
            .get(0).getRawStringContent())
            .isEqualTo("VAUClientHelloData");
    }

    @Test
    public void vauClientSigFin_shouldDecipherMessageWithCorrectKeyId() {
        final RbelElement vauMessage = rbelLogger.getMessageHistory().get(2)
            .findRbelPathMembers("$.body.FinishedData.content").get(0);
        assertThat(vauMessage.getFirst("keyId").get().getRawStringContent())
            .isEqualTo("f787a8db0b2e0d7c418ea20aba6125349871dfe36ab0f60a3d55bf4d1b556023");
    }

    @Test
    public void clientPayload_shouldParseEncapsulatedXml() {
        assertThat(rbelLogger.getMessageHistory().get(4)
            .findRbelPathMembers("$.body.message.Envelope.Body.sayHello.arg0.text")
            .get(0).getRawStringContent())
            .isEqualTo("hello from integration client");
    }

    @Test
    public void parentKeysForVauKeysShouldBeCorrect() {
        assertThat(rbelLogger.getMessageHistory().get(7)
            .findRbelPathMembers("$.body").get(0)
            .getFacetOrFail(RbelVauEpaFacet.class)
            .getKeyUsed())
            .get()
            .isInstanceOf(RbelVauKey.class)
            .extracting(key -> ((RbelVauKey) key).getParentKey())
            .extracting(RbelKey::getKeyName)
            .isEqualTo("prk_vauClientKeyPair");
    }

    private class TestFacet implements RbelFacet {
        @Override
        public List<RbelMultiMap> getChildElements() {
            return List.of();
        }
    }
}

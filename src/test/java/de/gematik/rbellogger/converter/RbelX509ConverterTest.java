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

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
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
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelX509ConverterTest {

    private RbelElement xmlMessage;

    @BeforeEach
    public void setUp() throws IOException {
        RbelOptions.activateRbelPathDebugging();
        xmlMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/xmlMessage.curl").getBytes(), null, null);
    }

    @SneakyThrows
    @Test
    public void shouldRenderCleanHtml() {
        FileUtils.writeStringToFile(new File("target/x509Message.html"),
            RbelHtmlRenderer.render(List.of(xmlMessage)));
    }

    @SneakyThrows
    @Test
    public void shouldBeAccessibleViaRbelPath() {
        final RbelElement certificateElement = xmlMessage.findElement("$..[?(@.subject=~'.*TEST-ONLY.*')]").get();

        assertThat(certificateElement)
            .isEqualTo(xmlMessage.findElement(
                "$.body.RegistryResponse.RegistryErrorList.RegistryError.jwtTag.text.header.x5c.0.content").get());
    }
}

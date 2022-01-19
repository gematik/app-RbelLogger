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

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelJsonFacet;
import de.gematik.rbellogger.data.facet.RbelJwtFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {

    @Test
    public void convertMessage_shouldGiveJsonBody() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks(
            "src/test/resources/sampleMessages/jsonMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertElement(curlMessage, null);

        assertThat(convertedMessage.getFirst("body").get().hasFacet(RbelJsonFacet.class))
            .isTrue();
    }

    @Test
    public void shouldRenderCleanHtml() throws IOException {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertElement(readCurlFromFileWithCorrectedLineBreaks(
                "src/test/resources/sampleMessages/idpEncMessage.curl"), null);
        convertedMessage.addFacet(RbelTcpIpMessageFacet.builder()
            .receiver(RbelElement.wrap(null, convertedMessage, new RbelHostname("recipient", 1)))
            .sender(RbelElement.wrap(null, convertedMessage, new RbelHostname("sender", 1)))
            .build());
    }

    @Test
    public void jsonMessageWithNestedJwt_shouldFindAndPresentNestedItems() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks(
            "src/test/resources/sampleMessages/getChallenge.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(curlMessage.getBytes(), null, null);

        FileUtils.writeStringToFile(new File("target/jsonNested.html"),
            RbelHtmlRenderer.render(List.of(convertedMessage)));

        assertThat(convertedMessage.getFirst("body").get().traverseAndReturnNestedMembers().stream()
            .filter(el -> el.hasFacet(RbelJwtFacet.class))
            .findAny())
            .isPresent();
    }
}

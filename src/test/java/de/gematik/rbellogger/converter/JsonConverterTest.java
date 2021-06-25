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

package de.gematik.rbellogger.converter;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.elements.RbelElement;
import de.gematik.rbellogger.data.elements.RbelHttpResponse;
import de.gematik.rbellogger.data.elements.RbelJsonElement;
import de.gematik.rbellogger.data.elements.RbelJwtElement;
import de.gematik.rbellogger.renderer.RbelMarkdownRenderer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {

    @Test
    public void convertMessage_shouldGiveJsonBody() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks(
            "src/test/resources/sampleMessages/jsonMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertElement(curlMessage);

        System.out.println(RbelMarkdownRenderer.render(convertedMessage));

        assertThat(((RbelHttpResponse) convertedMessage).getBody())
            .isInstanceOf(RbelJsonElement.class);
    }

    @Test
    public void jsonMessageWithNestedJwt_shouldFindAndPresentNestedItems() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks(
            "src/test/resources/sampleMessages/getChallenge.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertElement(curlMessage);

        System.out.println(RbelMarkdownRenderer.render(convertedMessage));
        final RbelJsonElement body = (RbelJsonElement) ((RbelHttpResponse) convertedMessage).getBody();

        assertThat(body.traverseAndReturnNestedMembers())
            .hasSize(7);

        assertThat(body.traverseAndReturnNestedMembers().values().stream()
            .filter(RbelJwtElement.class::isInstance)
            .findAny())
            .isPresent();
    }
}

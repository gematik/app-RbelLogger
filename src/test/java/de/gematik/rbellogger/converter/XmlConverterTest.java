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
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHttpResponse;
import de.gematik.rbellogger.data.RbelXmlElement;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XmlConverterTest {

    private String curlMessage;

    @BeforeEach
    public void setUp() throws IOException {
        curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/xmlMessage.curl");
    }

    @Test
    public void convertMessage_shouldGiveXmlBody() {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage.findRbelPathMembers("$.body").get(0))
            .isInstanceOf(RbelXmlElement.class);
    }

    @Test
    public void retrieveXmlAttribute_shouldReturnAttributeWithContent() {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage
            .findRbelPathMembers("$.body.RegistryResponse.status")
            .get(0).getContent())
            .isEqualTo("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Failure");
    }

    @Test
    public void retrieveListMemberAttribute() {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        final List<RbelElement> deepPathResults = convertedMessage
            .findRbelPathMembers("$.body.RegistryResponse.RegistryErrorList.RegistryError[0].errorCode");
        assertThat(convertedMessage.findRbelPathMembers("$..RegistryError.errorCode"))
            .containsAll(deepPathResults);

        assertThat(deepPathResults.get(0).getContent())
            .isEqualTo("XDSDuplicateUniqueIdInRegistry");
    }

    @Test
    public void retrieveTextContent() {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        final List<RbelElement> rbelPathResult = convertedMessage.findRbelPathMembers("$..RegistryError[0].text");

        assertThat(rbelPathResult).hasSize(1);
        assertThat(rbelPathResult.get(0).getContent().trim())
            .isEqualTo("text in element");
    }

    @Test
    public void diveIntoNestedJwt() {
        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        final List<RbelElement> rbelPathResult =
            convertedMessage.findRbelPathMembers("$..jwtTag.text.body.scopes_supported.0");

        assertThat(rbelPathResult).hasSize(1);
        assertThat(rbelPathResult.get(0).getContent().trim())
            .isEqualTo("openid");
    }
}

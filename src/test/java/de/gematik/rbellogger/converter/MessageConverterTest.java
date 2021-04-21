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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class MessageConverterTest {

    @Test
    public void convertMessage_shouldGiveCorrectType() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jsonMessage.curl"));

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter().convertMessage(curlMessage);

        assertThat(convertedMessage)
            .isInstanceOf(RbelHttpResponse.class);

    }

    @Test
    public void convertMessage_shouldGiveHeaderFields() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jsonMessage.curl"));

        final RbelHttpResponse convertedMessage = (RbelHttpResponse) RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        final Map<String, RbelElement> elementMap = convertedMessage.getHeader();
        assertThat(elementMap)
            .containsEntry("Content-Type", new RbelStringElement("application/json"))
            .hasSize(3);
    }

    @Test
    public void convertMessage_shouldGiveBodyAsJson() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jsonMessage.curl"));

        final RbelHttpResponse convertedMessage = (RbelHttpResponse) RbelLogger.build().getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage.getBody())
            .isInstanceOf(RbelJsonElement.class);
    }
}
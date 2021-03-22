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

package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHttpResponse;
import de.gematik.rbellogger.data.RbelJsonElement;
import de.gematik.rbellogger.data.RbelJwtElement;
import de.gematik.rbellogger.renderer.RbelMarkdownRenderer;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {

    @Test
    public void convertMessage_shouldGiveJsonBody() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jsonMessage.curl"));

        final RbelElement convertedMessage = RbelConverter.build().convertMessage(curlMessage);

        System.out.println(RbelMarkdownRenderer.render(convertedMessage));

        assertThat(((RbelHttpResponse) convertedMessage).getBody())
            .isInstanceOf(RbelJsonElement.class);
    }

    @Test
    public void jsonMessageWithNestedJwt_shouldFindAndPresentNestedItems() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/getChallenge.curl"));

        final RbelElement convertedMessage = RbelConverter.build().convertMessage(curlMessage);

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

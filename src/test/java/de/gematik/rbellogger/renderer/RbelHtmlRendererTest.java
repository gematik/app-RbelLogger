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

package de.gematik.rbellogger.renderer;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.RbelMessage;
import de.gematik.rbellogger.data.elements.RbelElement;
import de.gematik.rbellogger.data.elements.RbelHttpMessage;
import de.gematik.rbellogger.data.elements.RbelHttpResponse;
import de.gematik.rbellogger.data.elements.RbelJwtElement;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class RbelHtmlRendererTest {

    @Test
    public void convertToHtml() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter().convertElement(curlMessage);

        FileUtils.writeStringToFile(new File("target/out.html"),
            RbelHtmlRenderer.render(wrapHttpMessage(convertedMessage)), Charset.defaultCharset());
    }

    @Test
    public void valueShading() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelHttpResponse convertedMessage = (RbelHttpResponse) RbelLogger.build()
            .getRbelConverter().convertElement(curlMessage);
        convertedMessage.setNote("foobar Message");
        convertedMessage.getHeader().setNote("foobar Header");
        convertedMessage.getHeader().getChildNodes().stream()
            .forEach(element -> element.setNote("some note " + RandomStringUtils.randomAlphanumeric(30)));
        convertedMessage.getBody().setNote("foobar Body");
        ((RbelJwtElement) convertedMessage.getBody()).getHeader().setNote("foobar JWT Header");
        ((RbelJwtElement) convertedMessage.getBody()).getBody().setNote("foobar JWT Body");
        ((RbelJwtElement) convertedMessage.getBody()).getSignature().setNote("foobar Signature");

        final String convertedHtml = RbelHtmlRenderer.render(wrapHttpMessage(convertedMessage), new RbelValueShader()
            .addSimpleShadingCriterion("Date", "<halt ein date>")
            .addSimpleShadingCriterion("Content-Length", "<Die Länge. Hier %s>")
            .addSimpleShadingCriterion("exp", "<Nested Shading>")
        );

        assertThat(convertedHtml)
            .contains("&lt;halt ein date&gt;")
            .contains("&lt;Die Länge. Hier 2653&gt;")

            .contains("\\u003cNested Shading\\u003e");

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);
    }

    @Test
    public void advancedShading() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(curlMessage.getBytes(), null, null)
            .getHttpMessage();

        final String convertedHtml = RbelHtmlRenderer.render(wrapHttpMessage(convertedMessage), new RbelValueShader()
            .addJexlShadingCriterion("key == 'Version'", "<version: %s>")
            .addJexlShadingCriterion("key == 'nbf' && empty(element.parentNode)", "<nbf in JWT: %s>")
        );

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);

        assertThat(convertedHtml)
            .contains("&lt;version: 9.0.0&gt;")
            .contains("nbf-Wert in http header")
            .contains("\\u003cnbf in JWT: 1614339303\\u003e")
            .doesNotContain("nbf in JWT: nbf-Wert in http header");
    }

    @Test
    public void onlyServerNameKnown_shouldStillRender() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(curlMessage.getBytes(), new RbelHostname("foobar", 666), null)
            .getHttpMessage();

        final String convertedHtml = RbelHtmlRenderer.render(wrapHttpMessage(convertedMessage));

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);

        assertThat(convertedHtml)
            .contains("foobar:666")
            .doesNotContain("null");
    }

    private List<RbelMessage> wrapHttpMessage(RbelElement convertedMessage) {
        return List.of(RbelMessage.builder().httpMessage((RbelHttpMessage) convertedMessage).build());
    }
}

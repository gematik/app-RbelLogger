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

package de.gematik.rbellogger.renderer;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.RbelTcpIpMessageFacet;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

import de.gematik.rbellogger.data.facet.RbelNoteFacet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

public class RbelHtmlRendererTest {

    private static final RbelConverter RBEL_CONVERTER = RbelLogger.build()
        .getRbelConverter();
    private static final RbelHtmlRenderer RENDERER = new RbelHtmlRenderer();

    @Test
    public void convertToHtml() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter().convertElement(curlMessage, null);

        FileUtils.writeStringToFile(new File("target/out.html"),
            RbelHtmlRenderer.render(wrapHttpMessage(convertedMessage)), Charset.defaultCharset());
    }

    @Test
    public void valueShading() throws IOException {
        RENDERER.setRenderAsn1Objects(true);
        RENDERER.setRenderNestedObjectsWithoutFacetRenderer(true);
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RBEL_CONVERTER.convertElement(curlMessage, null);
        convertedMessage.addFacet(new RbelNoteFacet("foobar Message"));
        convertedMessage.getFirst("header").get().addFacet(new RbelNoteFacet("foobar Header"));
        convertedMessage.getFirst("header").get().getChildNodes().stream()
            .forEach(element -> {
                for (int i = 0; i < RandomUtils.nextInt(0, 4); i++) {
                    element.addFacet(new RbelNoteFacet("some note " + RandomStringUtils.randomAlphanumeric(30),
                        RbelNoteFacet.NoteStyling.values()[RandomUtils.nextInt(0, 3)]));
                }
            });
        convertedMessage.getFirst("body").get().addFacet(new RbelNoteFacet("foobar Body"));
        convertedMessage.findElement("$.body.header").get().addFacet(new RbelNoteFacet("foobar JWT Header"));
        convertedMessage.findElement("$.body.body").get().addFacet(new RbelNoteFacet("foobar JWT Body"));
        convertedMessage.findElement("$.body.signature").get().addFacet(new RbelNoteFacet("foobar Signature"));
        convertedMessage.findElement("$.body.body.jwks_uri").get().addFacet(new RbelNoteFacet("jwks_uri: note im JSON"));
        convertedMessage.findElement("$.body.body.jwks_uri").get().addFacet(new RbelNoteFacet("warnung", RbelNoteFacet.NoteStyling.WARN));
        convertedMessage.findElement("$.body.body.scopes_supported").get().addFacet(new RbelNoteFacet("scopes_supported: note an einem array"));

        final String convertedHtml = RENDERER.render(wrapHttpMessage(convertedMessage), new RbelValueShader()
            .addSimpleShadingCriterion("Date", "<halt ein date>")
            .addSimpleShadingCriterion("Content-Length", "<Die Länge. Hier %s>")
            .addSimpleShadingCriterion("exp", "<Nested Shading>")
            .addSimpleShadingCriterion("nbf", "\"foobar\"")
            .addSimpleShadingCriterion("iat", "&some&more\"stuff\"")
        );
        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);

        assertThat(convertedHtml)
            .contains("&lt;halt ein date&gt;")
            .contains("&lt;Die Länge. Hier 2653&gt;")

            .contains("\"&quot;foobar&quot;\"")
            .contains("&amp;some&amp;more&quot;stuff&quot;");
    }

    @Test
    public void advancedShading() throws IOException {
        RENDERER.setRenderAsn1Objects(true);
        RENDERER.setRenderNestedObjectsWithoutFacetRenderer(true);
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final String convertedHtml = RENDERER.render(wrapHttpMessage(RBEL_CONVERTER.convertElement(curlMessage, null)), new RbelValueShader()
            .addJexlShadingCriterion("key == 'Version'", "<version: %s>")
            .addJexlShadingCriterion("key == 'nbf' && empty(element.parentNode)", "<nbf in JWT: %s>")
        );

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);

        assertThat(convertedHtml)
            .contains("&lt;version: 9.0.0&gt;")
            .contains("nbf-Wert in http header")
            .contains("&lt;nbf in JWT: 1614339303&gt;")
            .doesNotContain("nbf in JWT: nbf-Wert in http header");
    }

    @Test
    public void onlyServerNameKnown_shouldStillRender() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(curlMessage.getBytes(), new RbelHostname("foobar", 666), null);

        final String convertedHtml = RENDERER.render(List.of(convertedMessage));

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);

        assertThat(convertedHtml)
            .contains("foobar:666")
            .doesNotContain("null");
    }

    private List<RbelElement> wrapHttpMessage(RbelElement convertedMessage) {
        convertedMessage.addFacet(RbelTcpIpMessageFacet.builder()
            .receiver(RbelElement.wrap(null, convertedMessage, new RbelHostname("recipient", 1)))
            .sender(RbelElement.wrap(null, convertedMessage, new RbelHostname("sender", 1)))
            .build());
        return List.of(convertedMessage);
    }
}

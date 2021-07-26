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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
        convertedMessage.setNote("foobar Message");
        convertedMessage.getFirst("header").get().setNote("foobar Header");
        convertedMessage.getFirst("header").get().getChildNodes().stream()
            .forEach(element -> element.setNote("some note " + RandomStringUtils.randomAlphanumeric(30)));
        convertedMessage.getFirst("body").get().setNote("foobar Body");
        convertedMessage.findRbelPathMembers("$.body.header").get(0).setNote("foobar JWT Header");
        convertedMessage.findRbelPathMembers("$.body.body").get(0).setNote("foobar JWT Body");
        convertedMessage.findRbelPathMembers("$.body.signature").get(0).setNote("foobar Signature");

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
package de.gematik.rbellogger.data;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class RbelPathTest {

    private RbelElement convertedMessage;

    @BeforeEach
    public void setUp() throws IOException {
        RbelJexlExecutor.activateJexlDebugging();
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/rbelPath.curl");

        convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(curlMessage.getBytes(), null, null);
    }

    @Test
    public void assertThatPathValueFollowsConvention() {
        assertThat(convertedMessage.findNodePath())
            .isEqualTo("");
        assertThat(convertedMessage.getFirst("header").get().findNodePath())
            .isEqualTo("header");
        assertThat(convertedMessage.getFirst("header").get().getChildNodes().get(0).findNodePath())
            .startsWith("header.");
    }

    @Test
    public void simpleRbelPath_shouldFindTarget() {
        assertThat(convertedMessage.findElement("$.header"))
            .get()
            .isSameAs(convertedMessage.getFacetOrFail(RbelHttpMessageFacet.class).getHeader());

        assertThat(convertedMessage.findRbelPathMembers("$.body.body.nbf"))
            .containsExactly(convertedMessage.getFirst("body").get()
                .getFirst("body").get()
                .getFirst("nbf").get()
                .getFirst("content").get());
    }

    @Test
    public void rbelPathEndingOnStringValue_shouldReturnNestedValue() {
        assertThat(convertedMessage.findRbelPathMembers("$.body.body.sso_endpoint")
            .get(0).getRawStringContent())
            .startsWith("http://");
    }

    @Test
    public void squareBracketRbelPath_shouldFindTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$.['body'].['body'].['nbf']"))
            .containsExactly(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get().
                getFirst("content").get());
    }

    @Test
    public void wildcardDescent_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$.body.[*].nbf"))
            .containsExactly(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get().
                getFirst("content").get());
    }

    @Test
    public void recursiveDescent_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..nbf"))
            .hasSize(2)
            .contains(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get().
                getFirst("content").get());
        assertThat(convertedMessage.findRbelPathMembers("$.body..nbf"))
            .hasSize(1)
            .contains(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get().
                getFirst("content").get());
    }

    @Test
    public void jexlExpression_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..[?(key=='nbf')]"))
            .hasSize(2)
            .contains(convertedMessage.getFirst("body").get()
                .getFirst("body").get()
                .getFirst("nbf").get()
                .getFirst("content").get());
    }

    @Test
    public void complexJexlExpression_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..[?(path=~'.*scopes_supported\\.\\d')]"))
            .hasSize(2);

        assertThat(convertedMessage.findRbelPathMembers("$.body.body..[?(path=~'.*scopes_supported\\.\\d')]"))
            .hasSize(2);
    }

    @Test
    public void findAllMembers() throws IOException {
        assertThat(convertedMessage.findRbelPathMembers("$..*"))
            .hasSize(168);

        FileUtils.writeStringToFile(new File("target/jsonNested.html"),
            RbelHtmlRenderer.render(List.of(convertedMessage)));

    }

    @Test
    public void findSingleElement_present() {
        assertThat(convertedMessage.findElement("$.body.body.authorization_endpoint"))
            .isPresent()
            .get()
            .isEqualTo(convertedMessage.findRbelPathMembers("$.body.body.authorization_endpoint").get(0));
    }

    @Test
    public void findSingleElement_notPresent_expectEmpty() {
        assertThat(convertedMessage.findElement("$.hfd7a89vufd"))
            .isEmpty();
    }

    @Test
    public void findSingleElementWithMultipleReturns_expectException() {
        assertThatThrownBy(() -> convertedMessage.findElement("$..*"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void eliminateContentInRbelPathResult() throws IOException {
        final String challengeMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/getChallenge.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(challengeMessage.getBytes(), null, null);

        assertThat(convertedMessage.findElement("$.body.challenge.signature").get())
            .isSameAs(convertedMessage.findElement("$.body.challenge.content.signature").get());
    }
}

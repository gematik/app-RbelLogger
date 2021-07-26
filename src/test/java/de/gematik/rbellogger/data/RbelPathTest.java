package de.gematik.rbellogger.data;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RbelPathTest {

    private RbelElement convertedMessage;

    @BeforeEach
    public void setUp() throws IOException {
        RbelJexlExecutor.activateJexlDebugging();
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

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
        assertThat(convertedMessage.findRbelPathMembers("$.header"))
            .containsExactly(convertedMessage.getFacetOrFail(RbelHttpMessageFacet.class).getHeader());

        assertThat(convertedMessage.findRbelPathMembers("$.body.body.nbf"))
            .containsExactly(convertedMessage.getFirst("body").get()
                .getFirst("body").get()
                .getFirst("nbf").get());
    }

    @Test
    public void squareBracketRbelPath_shouldFindTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$.['body'].['body'].['nbf']"))
            .containsExactly(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get());
    }

    @Test
    public void wildcardDescent_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$.body.[*].nbf"))
            .containsExactly(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get());
    }

    @Test
    public void recursiveDescent_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..nbf"))
            .hasSize(2)
            .contains(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get());
        assertThat(convertedMessage.findRbelPathMembers("$.body..nbf"))
            .hasSize(1)
            .contains(convertedMessage.getFirst("body").get().
                getFirst("body").get().
                getFirst("nbf").get());
    }

    @Test
    public void jexlExpression_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..[?(key=='nbf')]"))
            .hasSize(2)
            .contains(convertedMessage.getFirst("body").get()
                .getFirst("body").get()
                .getFirst("nbf").get());
    }

    @Test
    public void complexJexlExpression_shouldFindSpecificTarget() {
        assertThat(convertedMessage.findRbelPathMembers("$..[?(path=~'.*scopes_supported\\.\\d')]"))
            .hasSize(2);

        assertThat(convertedMessage.findRbelPathMembers("$.body.body..[?(path=~'.*scopes_supported\\.\\d')]"))
            .hasSize(2);
    }

    @Test
    public void findAllMembers() {
        assertThat(convertedMessage.findRbelPathMembers("$..*"))
            .hasSize(199);
    }
}

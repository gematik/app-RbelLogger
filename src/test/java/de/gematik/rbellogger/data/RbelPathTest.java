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

package de.gematik.rbellogger.data;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.util.RbelPathExecutor;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class RbelPathTest {

    private RbelElement convertedMessage;

    @BeforeEach
    public void setUp() throws IOException {
        RbelOptions.activateRbelPathDebugging();
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

    @Test
    public void successfulRequest_expectOnlyInitialTree() {
        final ListAppender<ILoggingEvent> listAppender = listFollowingLoggingEventsForClass(RbelPathExecutor.class);
        convertedMessage.findRbelPathMembers("$.body.header.kid");

        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getMessage)
            .filter(str -> str.startsWith("Resolving key")))
            .hasSize(3);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(str -> str.startsWith("Returning 1 result elements")))
            .hasSize(1);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(str -> str.contains("discSig")))
            .hasSize(1);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(str -> str.contains("$.body.header.kid")))
            .hasSize(2);
    }

    @Test
    public void successfulLongerRequest_treeSizeShouldBeAccordingly() {
        final ListAppender<ILoggingEvent> listAppender = listFollowingLoggingEventsForClass(RbelPathExecutor.class);
        convertedMessage.findRbelPathMembers("$.body.body.acr_values_supported.0.content");

        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .map(str -> str.split("http://localhost:8080/idpEnc/jwks.json").length - 1)
            .sorted(Comparator.reverseOrder())
            .findFirst().get())
            .isEqualTo(3);
    }

    @Test
    public void unsuccessfullyRequest_expectTreeOfLastSuccessfulPosition() {
        final ListAppender<ILoggingEvent> listAppender = listFollowingLoggingEventsForClass(RbelPathExecutor.class);
        convertedMessage.findRbelPathMembers("$.body.body.acr_values_supported.content");

        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getMessage)
            .filter(str -> str.startsWith("No more candidate-nodes in RbelPath execution!")))
            .hasSize(1);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(str -> str.contains("[$.body.body.acr_values_supported]")))
            .hasSize(1);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .map(str -> str.split("\\[0m\\n\\n").length)
            .sorted(Comparator.reverseOrder())
            .findFirst().get())
            .isEqualTo(1);
    }

    @Test
    public void unsuccessfulRequestWithAmbiguousFinalPosition_expectTreeOfAllCandidates() {
        final ListAppender<ILoggingEvent> listAppender = listFollowingLoggingEventsForClass(RbelPathExecutor.class);
        convertedMessage.findRbelPathMembers("$.body.body.*.foobar");

        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getMessage)
            .filter(str -> str.startsWith("No more candidate-nodes in RbelPath execution!")))
            .hasSize(1);
        assertThat(listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .map(str -> str.split("\\n\\n").length)
            .sorted(Comparator.reverseOrder())
            .findFirst().get())
            .isEqualTo(34);
    }

    @Test
    public void rbelPathWithReasonPhrase_shouldReturnTheValue() {
        assertThat(convertedMessage.findRbelPathMembers("$.reasonPhrase")
            .get(0).getRawStringContent())
            .isEqualTo("OK");
    }

    private ListAppender<ILoggingEvent> listFollowingLoggingEventsForClass(Class<RbelPathExecutor> clazz) {
        Logger fooLogger = (Logger) LoggerFactory.getLogger(clazz);
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);
        return listAppender;
    }
}

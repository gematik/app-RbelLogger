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

package de.gematik.rbellogger.modifier;

import static org.assertj.core.api.Assertions.assertThat;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import java.io.IOException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class RbelModifierTest extends AbstractModifierTest {

    @Test
    public void simpleHeaderReplace() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .name("blub")
            .targetElement("$.header.Version")
            .replaceWith("foobar")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(message.findElement("$.header.Version")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("9.0.0");
        assertThat(modifiedMessage.findElement("$.header.Version")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("foobar");
    }

    @Test
    public void responseCodeReplace() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .name("blub")
            .targetElement("$.responseCode")
            .replaceWith("666")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getResponseCode().getRawStringContent())
            .isEqualTo("666");
    }

    @Test
    public void reasonPhraseReplaceWithAnotherReasonPhrase() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/reasonPhraseMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith("foobar bar bar barsss")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo("foobar bar bar barsss");
    }

    @Test
    public void reasonPhraseReplaceWithEmptyString() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/reasonPhraseMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith("")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo(null);

        assertThat(modifiedMessage.getRawStringContent()).contains("HTTP/1.1 200\r\n"
            + "Cache-Control: max-age=300");
    }

    @Test
    public void reasonPhraseReplaceWithNull() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/reasonPhraseMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith(null)
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo(null);

        assertThat(modifiedMessage.getRawStringContent()).contains("HTTP/1.1 200\r\n"
            + "Cache-Control: max-age=300");
    }

    @Test
    public void reasonPhraseReplaceWithASpace() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/reasonPhraseMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith(" ")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo(null);

        assertThat(modifiedMessage.getRawStringContent()).contains("HTTP/1.1 200\r\n"
            + "Cache-Control: max-age=300");
    }

    @Test
    public void reasonPhraseAdd() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith("foobar bar bar barsss")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo("foobar bar bar barsss");
        assertThat(modifiedMessage.getRawStringContent()).contains("HTTP/1.1 200 foobar bar bar barsss\r\n"
            + "Version: 9.0.0");
    }

    @Test
    public void responseCodeAndReasonPhraseReplace() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/reasonPhraseMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.responseCode")
            .replaceWith("6666")
            .build());

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.reasonPhrase")
            .replaceWith("My favourite reasonphrase")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getResponseCode().getRawStringContent())
            .isEqualTo("6666");
        assertThat(modifiedMessage.getFacetOrFail(RbelHttpResponseFacet.class)
            .getReasonPhrase().getRawStringContent())
            .isEqualTo("My favourite reasonphrase");
    }

    @Test
    public void bodyCompleteReplace() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body")
            .replaceWith("someOtherBody")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpMessageFacet.class)
            .getBody().getRawStringContent())
            .isEqualTo("someOtherBody");
    }

    @Test
    public void replaceFieldInJson() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.keys.0.kid")
            .replaceWith("anotherKeyId")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findElement("$.body.keys.0.kid")
            .get().getRawStringContent())
            .isEqualTo("anotherKeyId");
        assertThat(modifiedMessage.findElement("$.body")
            .get().getRawStringContent())
            .contains("\"keys\"")
            .contains("\"kid\"")
            .contains("\"y\"");
    }

    @Test
    public void replaceHttpVerbInRequest() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/getRequest.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.method")
            .replaceWith("POST")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpRequestFacet.class)
            .getMethod().getRawStringContent())
            .isEqualTo("POST");
    }

    @Test
    public void modificationButWithConditionNeverTrue() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/getRequest.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.method")
            .replaceWith("POST")
            .condition("$.method == POST")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpRequestFacet.class)
            .getMethod().getRawStringContent())
            .isEqualTo("GET");
    }

    @Test
    public void modificationWithConditionWhichIsTrue() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/getRequest.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.method")
            .replaceWith("POST")
            .condition("$.method == 'GET'")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.getFacetOrFail(RbelHttpRequestFacet.class)
            .getMethod().getRawStringContent())
            .isEqualTo("POST");
    }

    @Test
    public void regexReplacement() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/xmlMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body")
            .regexFilter("ErrorSeverityType:((Error)|(Warning))")
            .replaceWith("ErrorSeverityType:Error")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findRbelPathMembers("$.body.RegistryResponse.RegistryErrorList.*.severity")
            .stream().map(RbelElement::getRawStringContent)
            .collect(Collectors.toList()))
            .containsExactly(
                "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error",
                "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error");
    }

    @Test
    public void targetElementDoesNotExist_modificationShouldConcludeWithoutException() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/getRequest.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.foobar")
            .replaceWith("novalue")
            .build());

        modifyMessageAndParseResponse(message);
    }

    @Test
    public void multipleModifications_shouldApplyAll() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jsonMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .name("blub")
            .targetElement("$.header.Version")
            .replaceWith("foobar")
            .build());
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.keys.0.kid")
            .replaceWith("anotherKeyId")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findElement("$.header.Version")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("foobar");

        assertThat(modifiedMessage.findElement("$.body.keys.0.kid"))
            .get().extracting(RbelElement::getRawStringContent)
            .isEqualTo("anotherKeyId");
    }

    @Test
    public void modifyRequestPath() throws IOException {
        String specialCaseParameter = RandomStringUtils.randomPrint(300);
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/getRequest.curl",
            in -> in.replace("?", "?first=parameter&"));
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.path.with.value")
            .replaceWith("anotherValue")
            .build());
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$..[?(content=='bar2')]")
            .replaceWith("bar3")
            .build());
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.path.noValueJustKey.value")
            .replaceWith("keyAtLast")
            .build());
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.path.first.value")
            .replaceWith(specialCaseParameter)
            .build());
        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findElement("$.path.with.value")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("anotherValue");
        assertThat(modifiedMessage.findElement("$.path")
            .map(RbelElement::getRawStringContent).get())
            .contains("foo=bar1")
            .contains("foo=bar3")
            .doesNotContain("foo=bar2");
        assertThat(modifiedMessage.findElement("$.path.first.value")
            .map(RbelElement::getRawStringContent).get())
            .contains(specialCaseParameter);
    }
}

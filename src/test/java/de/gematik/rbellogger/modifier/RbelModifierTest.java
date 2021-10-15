package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelModifierTest {

    private RbelLogger rbelLogger;

    @BeforeEach
    public void initRbelLogger() {
        RbelJexlExecutor.activateJexlDebugging();
        if (rbelLogger == null) {
            rbelLogger = RbelLogger.build();
        }
        rbelLogger.getRbelModifier().deleteAllModifications();
    }

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

    private RbelElement modifyMessageAndParseResponse(RbelElement message) {
        final RbelElement modifiedMessage = rbelLogger.getRbelModifier().applyModifications(message);
        return modifiedMessage;
    }

    private RbelElement readAndConvertCurlMessage(String fileName) throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks(fileName);
        return rbelLogger.getRbelConverter()
            .convertElement(curlMessage, null);
    }
}

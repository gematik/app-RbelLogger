package de.gematik.rbellogger.data;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelJexlTest {

    private RbelElement response;
    private RbelElement request;
    private RbelJexlExecutor jexlExecutor;

    @BeforeEach
    public void setUp() throws IOException {
        RbelJexlExecutor.activateJexlDebugging();

        response = RbelLogger.build().getRbelConverter()
            .parseMessage(readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/rbelPath.curl").getBytes(), null, null);
        request = RbelLogger.build().getRbelConverter()
            .parseMessage(readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/getRequest.curl").getBytes(), null, null);
        jexlExecutor = new RbelJexlExecutor();
    }

    @Test
    public void checkRequestMapElements() {
        assertThat(jexlExecutor.matchesAsJexlExpression(
            request, "isRequest", Optional.empty()))
            .isTrue();
        assertThat(jexlExecutor.matchesAsJexlExpression(
            request, "isResponse == false", Optional.empty()))
            .isTrue();
        assertThat(jexlExecutor.matchesAsJexlExpression(
            request, "request == message", Optional.empty()))
            .isTrue();
    }

    @Test
    public void checkResponseMapElements() {
        assertThat(jexlExecutor.matchesAsJexlExpression(
            response, "isRequest == false", Optional.empty()))
            .isTrue();
        assertThat(jexlExecutor.matchesAsJexlExpression(
            response, "isResponse", Optional.empty()))
            .isTrue();
        assertThat(jexlExecutor.matchesAsJexlExpression(
            response, "request != message", Optional.empty()))
            .isTrue();
    }

    @Test
    public void checkJexlParsingForDoubleHeaders() throws IOException {
        RbelElement doubleHeaderMessage = RbelLogger.build().getRbelConverter()
            .parseMessage(readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/doubleHeader.curl").getBytes(), null, null);

        assertThat(jexlExecutor.matchesAsJexlExpression(
            doubleHeaderMessage, "isResponse", Optional.empty()))
            .isTrue();
    }
}

package de.gematik.rbellogger.data;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static de.gematik.rbellogger.TestUtils.localhostWithPort;
import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelJexlTest {

    private RbelElement response;
    private RbelElement request;
    private RbelJexlExecutor jexlExecutor;

    @BeforeEach
    public void setUp() throws IOException {
        RbelOptions.activateJexlDebugging();

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

    @Test
    public void shouldFindReceiverPort() throws IOException {
        RbelElement request = RbelLogger.build().getRbelConverter().parseMessage(
            readCurlFromFileWithCorrectedLineBreaks("src/test/resources/sampleMessages/getRequest.curl").getBytes(),
            localhostWithPort(44444),
            localhostWithPort(5432));

        assertThat(jexlExecutor.matchesAsJexlExpression(
            request, "$.receiver.port == '5432'", Optional.empty()))
            .isTrue();
    }
}

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.RbelElement;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelBearerTokenConverterTest {

    @Test
    public void shouldFindJwtInBearerHeaderAttributer() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/bearerToken.curl");

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter().convertElement(curlMessage, null);

        assertThat(convertedMessage.findRbelPathMembers("$.header.Authorization.BearerToken"))
                .isNotEmpty();
    }
}
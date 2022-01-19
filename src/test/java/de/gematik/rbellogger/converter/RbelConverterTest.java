package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.facet.RbelHostnameFacet;
import de.gematik.rbellogger.data.facet.RbelJwtFacet;
import de.gematik.rbellogger.data.facet.RbelNoteFacet;
import de.gematik.rbellogger.exceptions.RbelConversionException;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RbelConverterTest {

    @Test
    public void errorDuringConversion_shouldBeIgnored() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        var rbelLogger = RbelLogger.build();
        rbelLogger.getRbelConverter().addConverter((el, c) -> {
            if (el.hasFacet(RbelJwtFacet.class)) {
                throw new RuntimeException("this exception should be ignored");
            }
        });

        var convertedMessage = rbelLogger.getRbelConverter()
            .parseMessage(curlMessage.getBytes(), null, null);

        FileUtils.writeStringToFile(new File("target/error.html"),
            new RbelHtmlRenderer()
                .doRender(rbelLogger.getMessageHistory()), Charset.defaultCharset());

        assertThat(convertedMessage.findElement("$.body").get().getFacet(RbelNoteFacet.class)
            .get().getValue())
            .contains("this exception should be ignored", this.getClass().getSimpleName());
    }

    @Test
    public void parseMessage_shouldFailBecauseContentIsNull() {
        byte[] content = null;
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            RbelLogger.build().getRbelConverter().parseMessage(content, null, null);
        });
    }
}
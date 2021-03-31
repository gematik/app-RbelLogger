package de.gematik.rbellogger;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.data.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class RbelLoggerTest {

    @Test
    public void addNoteToHeader() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelLogger rbelLogger = RbelLogger.build();
        rbelLogger.getValueShader().addJexlNoteCriterion("key == 'Version'", "Extra note");
        final RbelHttpResponse convertedMessage = (RbelHttpResponse) rbelLogger.getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage.getHeader().getChildElements().get("Version").getNote())
            .isEqualTo("Extra note");
    }

    @Test
    public void shouldConvertNullElement() {
        final RbelLogger rbelLogger = RbelLogger.build();
        final RbelElement convertedMessage = rbelLogger.getRbelConverter()
            .convertMessage(new RbelNullElement());

        assertThat(convertedMessage)
            .isInstanceOf(RbelNullElement.class);
    }

    @Test
    public void preConversionMapperToShadeUrls() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addPreConversionMapper(RbelStringElement.class, (path, context) -> {
                if (path.getContent().startsWith("localhost:8080")) {
                    return new RbelStringElement(path.getContent().replace("localhost:8080", "meinedomain.de"));
                } else {
                    return path;
                }
            }));
        final RbelHttpResponse convertedMessage = (RbelHttpResponse) rbelLogger.getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage.getHeader().getChildElements().get("Host").getContent())
            .isEqualTo("meinedomain.de");
    }

    @Test
    public void addNoteToHttpHeaderButNotBody() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelLogger rbelLogger = RbelLogger.build();
        rbelLogger.getValueShader().addJexlNoteCriterion("path == 'header'", "Header note");
        final RbelHttpResponse convertedMessage = (RbelHttpResponse) rbelLogger.getRbelConverter()
            .convertMessage(curlMessage);

        assertThat(convertedMessage.getHeader().getNote())
            .isEqualTo("Header note");
        assertThat(convertedMessage.getBody().getNote())
            .isNull();
    }
}
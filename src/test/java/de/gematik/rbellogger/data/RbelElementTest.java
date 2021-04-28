package de.gematik.rbellogger.data;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class RbelElementTest {
    @Test
    public void assertThatPathValueFollowsConvention() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelHttpResponse convertedMessage = (RbelHttpResponse) RbelLogger.build().getRbelConverter().convertMessage(curlMessage);

        assertThat(convertedMessage.findNodePath()).isEqualTo("");
        assertThat(convertedMessage.getHeader().findNodePath()).isEqualTo("header");
        assertThat(convertedMessage.getHeader().getChildNodes().get(0).findNodePath()).startsWith("header.");
    }
}
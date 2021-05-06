package de.gematik.rbellogger;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.*;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelKeyManager;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
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

        assertThat(convertedMessage.getHeader().getFirst("Version").get().getNote())
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

        assertThat(convertedMessage.getHeader().getFirst("Host").get().getContent())
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

    @SneakyThrows
    @Test
    public void multipleKeysWithSameId_shouldSelectCorrectOne() {
        final PCapCapture pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/deregisterPairing.pcap")
            .build();
        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addKey("IDP symmetricEncryptionKey",
                new SecretKeySpec(DigestUtils.sha256("falscherTokenKey"), "AES"),
                RbelKey.PRECEDENCE_KEY_FOLDER)
            .addKey("IDP symmetricEncryptionKey",
                new SecretKeySpec(DigestUtils.sha256("geheimerSchluesselDerNochGehashtWird"), "AES"),
                RbelKey.PRECEDENCE_KEY_FOLDER)
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addPostConversionListener(RbelJweElement.class, RbelKeyManager.RBEL_IDP_TOKEN_KEY_LISTENER)
            .addCapturer(pCapCapture)
        );

        pCapCapture.close();

        FileUtils.writeStringToFile(new File("target/pairingList.html"),
            new RbelHtmlRenderer()
                .doRender(rbelLogger.getMessageHistory()), Charset.defaultCharset());

        final RbelJweEncryptionInfo jweEncryptionInfo = (RbelJweEncryptionInfo) rbelLogger.getMessageHistory().get(9)
            .getFirst("header").get()
            .getFirst("Location").get()
            .getFirst("code").get()
            .getFirst("encryptionInfo").get();
        assertThat(jweEncryptionInfo.getDecryptedUsingKeyWithId())
            .isEqualTo("IDP symmetricEncryptionKey");
    }
}

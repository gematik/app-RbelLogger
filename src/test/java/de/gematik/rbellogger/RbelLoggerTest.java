package de.gematik.rbellogger;

import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelKeyManager;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelLoggerTest {

    @Test
    public void addNoteToHeader() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelLogger rbelLogger = RbelLogger.build();
        rbelLogger.getValueShader().addJexlNoteCriterion("key == 'Version'", "Extra note");
        final RbelHttpMessageFacet convertedMessage = rbelLogger.getRbelConverter()
                .parseMessage(curlMessage.getBytes(), null, null)
                .getFacetOrFail(RbelHttpMessageFacet.class);

        assertThat(convertedMessage.getHeader().getFirst("Version").get().getNote())
                .get()
                .isEqualTo("Extra note");
    }

    @Test
    public void preConversionMapperToShadeUrls() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
                .addPreConversionMapper(RbelElement.class, (element, context) -> {
                    if (element.getRawStringContent().startsWith("localhost:8080")) {
                        element.getParentNode();
                        return element.toBuilder()
                                .rawContent(element.getRawStringContent()
                                        .replace("localhost:8080", "meinedomain.de")
                                        .getBytes())
                                .build();
                    } else {
                        return element;
                    }
                }));
        final RbelHttpMessageFacet convertedMessage = rbelLogger.getRbelConverter()
                .parseMessage(curlMessage.getBytes(), null, null)
                .getFacetOrFail(RbelHttpMessageFacet.class);

        assertThat(convertedMessage.getHeader().getFirst("Host").get().getRawStringContent())
                .isEqualTo("meinedomain.de");
    }

    @Test
    public void addNoteToHttpHeaderButNotBody() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
                ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelLogger rbelLogger = RbelLogger.build();
        rbelLogger.getValueShader().addJexlNoteCriterion("path == 'header'", "Header note");
        final RbelHttpMessageFacet convertedMessage = rbelLogger.getRbelConverter()
                .parseMessage(curlMessage.getBytes(), null, null)
                .getFacetOrFail(RbelHttpMessageFacet.class);

        assertThat(convertedMessage.getHeader().getNote())
                .get()
                .isEqualTo("Header note");
        assertThat(convertedMessage.getBody().getNote())
                .isEmpty();
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
                .addPostConversionListener(RbelKeyManager.RBEL_IDP_TOKEN_KEY_LISTENER)
                .addCapturer(pCapCapture)
        );

        pCapCapture.close();

        FileUtils.writeStringToFile(new File("target/pairingList.html"),
                new RbelHtmlRenderer()
                        .doRender(rbelLogger.getMessageHistory()), Charset.defaultCharset());

        assertThat(rbelLogger.getMessageHistory().get(9)
                .findRbelPathMembers("$.header.Location.code.value.encryptionInfo.decryptedUsingKeyWithId")
                .get(0)
                .seekValue())
                .get()
                .isEqualTo("IDP symmetricEncryptionKey");
    }
}

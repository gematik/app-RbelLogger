package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.Base64;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class VauModifierTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private RbelLogger rbelLogger;

    @BeforeEach
    public void initRbelLogger() {
        RbelOptions.activateJexlDebugging();
        if (rbelLogger == null) {
            rbelLogger = RbelLogger.build(
                new RbelConfiguration()
                    .addInitializer(new RbelKeyFolderInitializer("src/test/resources")));
        }
        rbelLogger.getRbelModifier().deleteAllModifications();
    }

    @Test
    public void modifyErpVauRequestBody() throws IOException {
        final RbelElement message = readAndConvertRawMessage("src/test/resources/vauErpRequest.b64");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.message.body")
            .replaceWith("<New>Vau inner body</New>")
            .build());

        final RbelElement modifiedMessage = rbelLogger.getRbelModifier().applyModifications(message);

        assertThat(modifiedMessage.findElement("$.body.message.body.New.text")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("Vau inner body");
    }

    @Test
    public void modifyErpVauResponseBody() throws IOException {
        rbelLogger.getRbelKeyManager().addKey("secretKey",
            new SecretKeySpec(Base64.getDecoder().decode("dGPgkcT15xeXhORNsgc83A=="), "AES"), 0);
        final RbelElement message = readAndConvertRawMessage("src/test/resources/vauErpResponse.b64");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.message.body")
            .replaceWith("<New>Vau inner body</New>")
            .build());

        final RbelElement modifiedMessage = rbelLogger.getRbelModifier().applyModifications(message);
        assertThat(modifiedMessage.findElement("$.body.message.body.New.text")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("Vau inner body");
    }

    @Test
    public void modifyEpaVauRequestBody() throws IOException {
        var pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/vauFlow.pcap")
            .filter("")
            .build();
        rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addCapturer(pCapCapture)
        );
        pCapCapture.close();
        final RbelElement message = rbelLogger.getMessageHistory().get(4);

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.message")
            .replaceWith("<New>Vau inner body</New>")
            .build());

        final RbelElement modifiedMessage = rbelLogger.getRbelModifier().applyModifications(message);

        assertThat(modifiedMessage.findElement("$.body.message.New.text")
            .map(RbelElement::getRawStringContent))
            .get()
            .isEqualTo("Vau inner body");
    }

    private RbelElement readAndConvertRawMessage(String fileName) throws IOException {
        String rawMessage = FileUtils.readFileToString(new File(fileName), Charset.defaultCharset());
        return rbelLogger.getRbelConverter()
            .convertElement(Base64.getDecoder().decode(rawMessage), null);
    }
}
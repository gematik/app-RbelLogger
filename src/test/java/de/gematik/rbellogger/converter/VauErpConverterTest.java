package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.captures.RbelFileReaderCapturer;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.Base64;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class VauErpConverterTest {

    private static PCapCapture pCapCapture;
    private static RbelLogger rbelLogger;

    @BeforeAll
    public static void setUp() {
        System.out.println("Initializing...");
        pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/rezepsFiltered.pcap")
            .filter("")
            .build();
        rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addCapturer(pCapCapture)
        );
        System.out.println("cont init...");
        pCapCapture.close();
        System.out.println("Initialized!");
    }

    @SneakyThrows
    @Test
    public void shouldRenderCleanHtml() {
        FileUtils.writeStringToFile(new File("target/vauErp.html"),
            RbelHtmlRenderer.render(rbelLogger.getMessageHistory()));
    }

    @Test
    public void testNestedRbelPathIntoErpRequest() {
        assertThat(rbelLogger.getMessageHistory().get(52)
            .findRbelPathMembers("$.body.message.body.Parameters.parameter.valueCoding.system.value")
            .get(0).getRawStringContent())
            .isEqualTo("https://gematik.de/fhir/CodeSystem/Flowtype");
    }

    @Test
    public void fixedSecretKeyOnly() throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode("krTNhsSUEfXvy6BZFp5G4g==");
        RbelLogger rbelLogger = RbelLogger.build();
        final RbelFileReaderCapturer fileReaderCapturer = new RbelFileReaderCapturer(rbelLogger.getRbelConverter(),
            "src/test/resources/rezeps_traffic_krTNhsSUEfXvy6BZFp5G4g==.tgr");
        rbelLogger.getRbelKeyManager().addKey("VAU Secret Key krTNhsSUEfXvy6BZFp5G4g",
            new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES"), 0);
        fileReaderCapturer.initialize();
        fileReaderCapturer.close();

        assertThat(rbelLogger.getMessageHistory().get(47)
            .findElement("$.body.keyId")
            .get().seekValue(String.class).get())
            .isEqualTo("VAU Secret Key krTNhsSUEfXvy6BZFp5G4g");
    }

    @Test
    public void testNestedRbelPathIntoErpVauResponse() {
        assertThat(rbelLogger.getMessageHistory().get(54)
            .findRbelPathMembers("$.body.message.body.Task.identifier.system.value")
            .stream().map(RbelElement::getRawStringContent).collect(Collectors.toList()))
            .containsExactly("https://gematik.de/fhir/NamingSystem/PrescriptionID",
                "https://gematik.de/fhir/NamingSystem/AccessCode");
    }

    @Test
    public void testNestedRbelPathIntoSignedErpVauMessage() {
//          assertThat(rbelLogger.getMessageHistory().get(95)
//            .findRbelPathMembers("$.body.message.body.Bundle.entry.resource.Binary.data.value.1.content.2.1.content")
//            .get(0).getFacet(RbelXmlElement.class))
//            .isPresent();
    }
}

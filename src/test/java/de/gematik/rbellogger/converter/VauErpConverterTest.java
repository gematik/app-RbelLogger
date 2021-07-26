package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VauErpConverterTest {

    private PCapCapture pCapCapture;
    private RbelLogger rbelLogger;

    @BeforeEach
    public void setUp() {
        pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/rezepsFiltered.pcap")
            .filter("")
            .build();
        rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addCapturer(pCapCapture)
        );
        pCapCapture.close();
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
    public void testNestedRbelPathIntoErpVauResponse() {
        assertThat(rbelLogger.getMessageHistory().get(54)
            .findRbelPathMembers("$.body.message.body.Task.identifier.system.value")
            .stream().map(RbelElement::getRawStringContent).collect(Collectors.toList()))
            .containsExactly("https://gematik.de/fhir/NamingSystem/PrescriptionID",
                "https://gematik.de/fhir/NamingSystem/AccessCode");
    }

    @Test
    public void testNestedRbelPathIntoSignedErpVauMessage() {
//TODO wartet auf asn1
//          assertThat(rbelLogger.getMessageHistory().get(95)
//            .findRbelPathMembers("$.body.message.body.Bundle.entry.resource.Binary.data.value.1.content.2.1.content")
//            .get(0).getFacet(RbelXmlElement.class))
//            .isPresent();
    }
}

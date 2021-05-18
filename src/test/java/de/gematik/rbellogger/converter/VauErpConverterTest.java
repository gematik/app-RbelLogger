/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        assertThat(rbelLogger.getMessageHistory().get(55)
            .findRbelPathMembers("$.body.message.body.Parameters.parameter.valueCoding.system.value")
            .get(0).getContent())
            .isEqualTo("https://gematik.de/fhir/CodeSystem/Flowtype");
    }

    @Test
    public void testNestedRbelPathIntoErpVauResponse() {
        assertThat(rbelLogger.getMessageHistory().get(57)
            .findRbelPathMembers("$.body.message.body.Task.identifier.system.value")
            .stream().map(RbelElement::getContent).collect(Collectors.toList()))
            .containsExactly("https://gematik.de/fhir/NamingSystem/PrescriptionID",
                "https://gematik.de/fhir/NamingSystem/AccessCode");
    }
}

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
import de.gematik.rbellogger.data.RbelJsonElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VauConverterTest {

    private PCapCapture pCapCapture;
    private RbelLogger rbelLogger;

    @BeforeEach
    public void setUp() {
        pCapCapture = PCapCapture.builder()
            .pcapFile("src/test/resources/vauFlow.pcap")
//            .pcapFile("out.pcap")
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
    public void convertMessage_shouldGiveXmlBody() {
        assertThat(rbelLogger.getMessageHistory())
            .hasSize(8);
        assertThat(rbelLogger.getMessageHistory().get(1).getFirst("body").get())
            .isInstanceOf(RbelJsonElement.class);
//        FileUtils.writeStringToFile(new File("target/blub.html"), RbelHtmlRenderer.render(rbelLogger.getMessageHistory()));
    }
}

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

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelMessage;
import de.gematik.rbellogger.data.elements.RbelAsn1Element;
import de.gematik.rbellogger.data.elements.RbelElement;
import de.gematik.rbellogger.data.elements.RbelUriElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RbelAsn1ConverterTest {

    private PCapCapture pCapCapture;
    private static RbelLogger rbelLogger;

    @BeforeAll
    public static void initRbelLogger() {
        rbelLogger = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
        );
    }

    public void parseRezepsCapture() {
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
        parseRezepsCapture();
        FileUtils.writeStringToFile(new File("target/asn1TestRender.html"),
            RbelHtmlRenderer.render(rbelLogger.getMessageHistory()));
    }

    @Test
    public void checkXmlInPkcs7InXml() throws IOException {
        // check OID
        final RbelElement convertMessage = rbelLogger.getRbelConverter().convertElement(
            readCurlFromFileWithCorrectedLineBreaks("src/test/resources/xmlWithNestedPkcs7.curl")
        );
        assertThat(convertMessage
            .findRbelPathMembers("$..author.type.value")
            .stream().map(RbelElement::getContent).collect(Collectors.toList()))
            .contains("Practitioner");
    }

    @Test
    public void testVariousRbelPathInPcap() {
        parseRezepsCapture();
        // check OID
        final RbelMessage rbelMessage = rbelLogger.getMessageHistory().get(58);
        assertThat(rbelMessage
            .findRbelPathMembers("$.body.0.2.0")
            .get(0).getContent())
            .isEqualTo("1.2.840.10045.4.3.2");

        // check X509-Version (Tagged-sequence)
        assertThat(rbelMessage
            .findRbelPathMembers("$.body.0.0.content")
            .get(0).getContent())
            .isEqualTo("2");

        // check OCSP URL
        assertThat(rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0.1.content.content")
            .get(0))
            .isInstanceOf(RbelUriElement.class);

        assertThat(((RbelAsn1Element) rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0")
            .get(0)).getAsn1Element())
            .isInstanceOf(ASN1Sequence.class);

        assertThat(((RbelAsn1Element) rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0")
            .get(0)).getAsn1Element())
            .isInstanceOf(ASN1Sequence.class);

        assertThat(((RbelAsn1Element) rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0.1")
            .get(0)).getAsn1Element())
            .isInstanceOf(ASN1TaggedObject.class);

        assertThat(rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0.1.tag")
            .get(0).getContent())
            .isEqualTo("6");

        assertThat(rbelMessage
            .findRbelPathMembers("$.body.0.7.content.3.1.content.0.1.content.content")
            .get(0).getContent())
            .isEqualTo("http://ehca.gematik.de/ocsp/");

        // Parse y-coordinate of signature (Nested in BitString)
        assertThat(rbelMessage
            .findRbelPathMembers("$.body.2.content.1").get(0)
            .getContent())
            .startsWith("9528585714247878020400211740123936754253798904841060501006300662224159");
    }
}

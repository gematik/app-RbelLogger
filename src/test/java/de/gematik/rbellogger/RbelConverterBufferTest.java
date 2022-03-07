/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.rbellogger;

import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelNoteFacet;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import static org.assertj.core.api.Assertions.assertThat;

public class RbelConverterBufferTest {

    @Test
    public void emptyBuffer_shouldNotContainMessages() throws IOException {
        final String curlMessage = readCurlFromFileWithCorrectedLineBreaks
            ("src/test/resources/sampleMessages/jwtMessage.curl");

        final RbelLogger rbelLogger = RbelLogger.build(RbelConfiguration.builder()
            .manageBuffer(true)
            .rbelBufferSizeInMb(0)
            .build());
        final RbelElement convertedMessage = rbelLogger.getRbelConverter()
            .parseMessage(curlMessage.getBytes(), null, null);

        assertThat(convertedMessage.findRbelPathMembers("$..*"))
            .hasSizeGreaterThan(30);
        assertThat(rbelLogger.getMessageHistory())
            .isEmpty();
    }
}

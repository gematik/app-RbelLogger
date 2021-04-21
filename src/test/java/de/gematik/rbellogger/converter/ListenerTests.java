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
import de.gematik.rbellogger.data.RbelHttpResponse;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class ListenerTests {

    @Test
    public void simpleMessageWithCounterListener_shouldBeTriggered() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jsonMessage.curl"));

        final AtomicInteger callCounter = new AtomicInteger(0);

        final RbelConverter rbelConverter = RbelLogger.build().getRbelConverter();
        rbelConverter.registerListener(RbelHttpResponse.class, (m, c) -> callCounter.incrementAndGet());
        rbelConverter.convertMessage(curlMessage);

        assertThat(callCounter.get()).isEqualTo(1);
    }
}

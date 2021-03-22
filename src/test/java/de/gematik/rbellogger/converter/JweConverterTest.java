/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.apps.PCapCapture;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JweConverterTest {

    @Test
    @Disabled
    public void shouldConvertJwe() {
        final RbelConverter rbelConverter = RbelConverter.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources")));
        PCapCapture.builder()
            .pcapFile("src/test/resources/ssoTokenFlow.pcap")
            .printMessageToSystemOut(false)
            .rbel(rbelConverter)
            .build()
            .run();

        final RbelHttpRequest postChallengeResponse = rbelConverter.getMessageHistory().stream()
            .filter(RbelHttpRequest.class::isInstance)
            .map(RbelHttpRequest.class::cast)
            .filter(request -> request.getPath().getBasicPath().getContent().equals("/sign_response")
                && request.getMethod().equals("POST"))
            .findFirst().get();

        final RbelJweElement signedChallenge = (RbelJweElement) ((RbelMapElement) postChallengeResponse.getBody())
            .getElementMap()
            .get("signed_challenge");
        assertThat(signedChallenge)
            .isInstanceOf(RbelJweElement.class);
        assertThat(signedChallenge.getHeader())
            .isInstanceOf(RbelJsonElement.class);
        assertThat(signedChallenge.getBody())
            .isInstanceOf(RbelJwtElement.class);
    }
}

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

package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelJsonFacet;
import de.gematik.rbellogger.data.facet.RbelJweFacet;
import de.gematik.rbellogger.data.facet.RbelJwtFacet;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class JweConverterTest {

    @Test
    @SneakyThrows
    public void shouldConvertJwe() {
        final RbelLogger rbelConverter = RbelLogger.build(new RbelConfiguration()
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addCapturer(PCapCapture.builder()
                .pcapFile("src/test/resources/ssoTokenFlow.pcap")
                .printMessageToSystemOut(false)
                .build()));
        rbelConverter.getRbelCapturer().close();

        final RbelElement postChallengeResponse = rbelConverter.getMessageHistory().stream()
            .filter(e -> e.hasFacet(RbelHttpRequestFacet.class))
            .filter(request -> request.getFacet(RbelHttpRequestFacet.class).get()
                .getPath().getRawStringContent().contains("/sign_response")
                && request.getFacet(RbelHttpRequestFacet.class).get().getMethod().getRawStringContent().equals("POST"))
            .findFirst().get();

        final RbelElement signedChallenge = postChallengeResponse.findRbelPathMembers("$..signed_challenge").get(0);
        assertThat(signedChallenge.hasFacet(RbelJweFacet.class))
            .isTrue();
        assertThat(signedChallenge.getFirst("header").get().hasFacet(RbelJsonFacet.class))
            .isTrue();
        assertThat(signedChallenge.getFirst("body").get().hasFacet(RbelJwtFacet.class))
            .isTrue();
    }
}

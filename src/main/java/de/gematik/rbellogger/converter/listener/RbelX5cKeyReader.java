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

package de.gematik.rbellogger.converter.listener;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.*;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.util.CryptoLoader;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RbelX5cKeyReader implements BiConsumer<RbelElement, RbelConverter> {

    @Override
    public void accept(RbelElement rbelElement, RbelConverter converter) {
        final Optional<RbelMapElement> keyMap = Optional.ofNullable(rbelElement)
            .map(RbelMapElement.class::cast)
            .filter(map -> map.getElementMap().containsKey("x5c"));
        if (keyMap.isPresent()) {
            final Optional<byte[]> certificateData = getX509Certificate(keyMap);
            final Optional<String> keyId = getKeyId(keyMap);
            if (keyId.isPresent() && certificateData.isPresent()) {
                try {
                    final X509Certificate certificate = CryptoLoader
                        .getCertificateFromPem(certificateData.get());
                    converter.getRbelKeyManager()
                        .addKey(keyId.get(), certificate.getPublicKey(), RbelKey.PRECEDENCE_X5C_HEADER_VALUE);
                } catch (Exception e) {
                    log.warn("Exception while extracting X5C: {}", e);
                }
            }
        }
    }

    private Optional<String> getKeyId(Optional<RbelMapElement> keyMap) {
        return keyMap
            .filter(map -> map.getElementMap().containsKey("kid"))
            .map(map -> map.getElementMap().get("kid"))
            .map(RbelElement::getContent);
    }

    private Optional<byte[]> getX509Certificate(Optional<RbelMapElement> keyMap) {
        return keyMap
            .map(map -> map.getElementMap().get("x5c"))
            .filter(RbelJsonElement.class::isInstance)
            .map(RbelJsonElement.class::cast)
            .map(RbelJsonElement::getJsonElement)
            .filter(RbelListElement.class::isInstance)
            .map(RbelListElement.class::cast)
            .map(RbelListElement::getElementList)
            .stream()
            .flatMap(List::stream)
            .filter(RbelAsn1Element.class::isInstance)
            .map(RbelAsn1Element.class::cast)
            .map(RbelAsn1Element::getEncoded)
            .findFirst();
    }
}

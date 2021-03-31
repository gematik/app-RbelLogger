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

import de.gematik.rbellogger.converter.brainpool.BrainpoolCurves;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelJwtElement;
import de.gematik.rbellogger.data.RbelJwtSignature;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jose4j.json.JsonUtil;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

public class RbelJwtConverter implements RbelConverterPlugin {

    static {
        BrainpoolCurves.init();
    }

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            final JsonWebSignature jsonWebSignature = initializeJws(rbel);
            JsonUtil.parseJson(jsonWebSignature.getHeaders().getFullHeaderAsJsonString());
            return true;
        } catch (final JoseException e) {
            return false;
        }
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        final JsonWebSignature jsonWebSignature = initializeJws(rbel);

        final RbelElement headerElement = context
            .convertMessage(jsonWebSignature.getHeaders().getFullHeaderAsJsonString());
        final RbelElement bodyElement = context.convertMessage(jsonWebSignature.getUnverifiedPayload());
        final RbelJwtSignature signature = context.getRbelKeyManager().getAllKeys()
            .map(rbelKey -> verifySig(jsonWebSignature, rbelKey.getKey(), rbelKey.getKeyName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findAny()
            .or(() -> tryToGetKeyFromX5cHeaderClaim(jsonWebSignature)
                .map(key -> verifySig(jsonWebSignature, key, "x5c-header certificate"))
                .filter(Optional::isPresent)
                .map(Optional::get)
            )
            .orElseGet(() -> new RbelJwtSignature(false, null));

        return new RbelJwtElement(headerElement, bodyElement, signature);
    }

    @SneakyThrows
    private Optional<PublicKey> tryToGetKeyFromX5cHeaderClaim(JsonWebSignature jsonWebSignature) {
        return Optional.ofNullable(jsonWebSignature.getCertificateChainHeaderValue())
            .map(list -> list.get(0))
            .map(X509Certificate::getPublicKey);
    }

    @SneakyThrows
    private JsonWebSignature initializeJws(RbelElement rbel) {
        final JsonWebSignature jsonWebSignature = new JsonWebSignature();
        jsonWebSignature.setCompactSerialization(rbel.getContent());
        return jsonWebSignature;
    }

    private Optional<RbelJwtSignature> verifySig(final JsonWebSignature jsonWebSignature, final Key key,
        final String keyId) {
        try {
            jsonWebSignature.setKey(key);
            tryToGetKeyFromX5cHeaderClaim(jsonWebSignature);
            if (jsonWebSignature.verifySignature()) {
                return Optional.of(new RbelJwtSignature(true, keyId));
            } else {
                return Optional.empty();
            }
        } catch (final JoseException e) {
            return Optional.empty();
        }
    }
}

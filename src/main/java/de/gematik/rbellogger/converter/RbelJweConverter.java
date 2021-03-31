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
import de.gematik.rbellogger.data.RbelJweElement;
import de.gematik.rbellogger.data.RbelJweEncryptionInfo;
import de.gematik.rbellogger.data.RbelStringElement;
import de.gematik.rbellogger.key.RbelKey;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;

public class RbelJweConverter implements RbelConverterPlugin {

    static {
        BrainpoolCurves.init();
    }

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        return initializeJwe(rbel).isPresent();
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        final JsonWebEncryption jwe = initializeJwe(rbel).get();

        final Optional<Pair<String, String>> correctKeyAndPayload = findCorrectKeyAndReturnPayload(context, jwe);
        if (correctKeyAndPayload.isEmpty()) {
            return new RbelJweElement(context.convertMessage(jwe.getHeaders().getFullHeaderAsJsonString()),
                new RbelStringElement("<Encrypted Payload>"), new RbelJweEncryptionInfo(false, null));
        }

        return new RbelJweElement(context.convertMessage(jwe.getHeaders().getFullHeaderAsJsonString()),
            context.convertMessage(correctKeyAndPayload.get().getValue()),
            new RbelJweEncryptionInfo(true, correctKeyAndPayload.get().getKey()));
    }

    private Optional<Pair<String, String>> findCorrectKeyAndReturnPayload(RbelConverter context,
        JsonWebEncryption jwe) {
        for (RbelKey keyEntry : context.getRbelKeyManager().getAllKeys().collect(Collectors.toList())) {
            try {
                jwe.setKey(keyEntry.getKey());
                return Optional.of(Pair.of(keyEntry.getKeyName(), jwe.getPayload()));
            } catch (Exception e) {
                continue;
            }
        }
        return Optional.empty();
    }

    @SneakyThrows
    private Optional<JsonWebEncryption> initializeJwe(RbelElement rbel) {
        final JsonWebEncryption receiverJwe = new JsonWebEncryption();

        receiverJwe.setDoKeyValidation(false);
        receiverJwe.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);

        try {
            receiverJwe.setCompactSerialization(rbel.getContent());
            receiverJwe.getHeaders();
            return Optional.ofNullable(receiverJwe);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}

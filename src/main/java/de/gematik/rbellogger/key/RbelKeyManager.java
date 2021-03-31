package de.gematik.rbellogger.key;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelJsonElement;
import de.gematik.rbellogger.data.RbelJweElement;
import de.gematik.rbellogger.data.RbelMapElement;
import java.security.Key;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;

public class RbelKeyManager {

    public static final BiConsumer<RbelJweElement, RbelConverter> RBEL_IDP_TOKEN_KEY_LISTENER = (element, converter) ->
        Optional.ofNullable(element.getBody())
            .filter(RbelJsonElement.class::isInstance)
            .map(RbelJsonElement.class::cast)
            .map(json -> json.getJsonElement())
            .filter(RbelMapElement.class::isInstance)
            .map(RbelMapElement.class::cast)
            .map(map -> map.getChildElements())
            .filter(map -> map.containsKey("token_key"))
            .map(map -> map.get("token_key"))
            .map(tokenB64 -> Base64.getUrlDecoder().decode(tokenB64.getContent()))
            .map(tokenKeyBytes -> new SecretKeySpec(tokenKeyBytes, "AES"))
            .ifPresent(aesKey -> converter.getRbelKeyManager().addKey("token_key", aesKey, RbelKey.PRECEDENCE_KEY_FOLDER));

    private final Map<String, RbelKey> keyIdToKeyDatabase = new HashMap<>();

    public RbelKeyManager addAll(Map<String, RbelKey> keys) {
        keyIdToKeyDatabase.putAll(keys);
        return this;
    }

    public void addKey(String keyId, Key key, int precedence) {
        keyIdToKeyDatabase.put(keyId, RbelKey.builder()
            .keyName(keyId)
            .key(key)
            .precedence(precedence)
            .build());
    }

    public Stream<RbelKey> getAllKeys() {
        return keyIdToKeyDatabase.values()
            .stream()
            .sorted(Comparator.comparing(RbelKey::getPrecedence));
    }
}

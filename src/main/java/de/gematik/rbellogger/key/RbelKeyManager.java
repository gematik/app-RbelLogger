package de.gematik.rbellogger.key;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.elements.RbelJsonElement;
import de.gematik.rbellogger.data.elements.RbelJweElement;
import java.security.Key;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RbelKeyManager {

    public static final BiConsumer<RbelJweElement, RbelConverter> RBEL_IDP_TOKEN_KEY_LISTENER = (element, converter) ->
        Optional.ofNullable(element.getBody())
            .filter(RbelJsonElement.class::isInstance)
            .map(RbelJsonElement.class::cast)
            .map(json -> json.getJsonElement())
            .map(map -> map.getFirst("token_key"))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(tokenB64 -> {
                try {
                    return Base64.getUrlDecoder().decode(tokenB64.getContent());
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .map(tokenKeyBytes -> new SecretKeySpec(tokenKeyBytes, "AES"))
            .ifPresent(
                aesKey -> converter.getRbelKeyManager().addKey("token_key", aesKey, RbelKey.PRECEDENCE_KEY_FOLDER));

    private final List<RbelKey> keyList = new ArrayList<>();

    public RbelKeyManager addAll(Map<String, RbelKey> keys) {
        keyList.addAll(keys.values());
        return this;
    }

    public void addKey(RbelKey rbelKey) {
        if (rbelKey.getKey() == null) {
            return;
        }

        if (keyIsPresentInList(rbelKey.getKey())) {
            log.debug("Skipping adding key: Key is already known!");
        }

        keyList.add(rbelKey);
    }

    public void addKey(String keyId, Key key, int precedence) {
        if (keyIsPresentInList(key)) {
            log.debug("Skipping adding key: Key is already known!");
        }

        keyList.add(RbelKey.builder()
            .keyName(keyId)
            .key(key)
            .precedence(precedence)
            .build());

        log.info("Added key {} (Now there are {} keys known)", keyId, keyList.size());
    }

    private boolean keyIsPresentInList(Key key) {
        return keyList.stream()
            .map(RbelKey::getKey)
            .map(Key::getEncoded)
            .filter(oldKey -> Arrays.equals(oldKey, key.getEncoded()))
            .findAny().isPresent();
    }

    public Stream<RbelKey> getAllKeys() {
        return keyList
            .stream()
            .sorted(Comparator.comparing(RbelKey::getPrecedence));
    }

    public Optional<RbelKey> findKeyByName(String keyName) {
        return getAllKeys()
            .filter(candidate -> candidate.getKeyName() != null)
            .filter(candidate -> candidate.getKeyName().equals(keyName))
            .findFirst();
    }
}

package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.converter.brainpool.BrainpoolCurves;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.elements.RbelJwtSignature;
import de.gematik.rbellogger.data.facet.RbelJwtFacet;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelKeyManager;
import de.gematik.rbellogger.util.JsonUtils;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

@AllArgsConstructor
public class RbelJwtWriter implements RbelElementWriter {

    static {
        BrainpoolCurves.init();
    }

    private final RbelKeyManager rbelKeyManager;

    @Override
    public boolean canWrite(RbelElement oldTargetElement) {
        return oldTargetElement.hasFacet(RbelJwtFacet.class);
    }

    @Override
    public String write(RbelElement oldTargetElement, RbelElement oldTargetModifiedChild, String newContent) {
        final RbelJwtFacet jwtFacet = oldTargetElement.getFacetOrFail(RbelJwtFacet.class);

        return createUpdatedJws(oldTargetModifiedChild, newContent, jwtFacet);
    }

    private String createUpdatedJws(RbelElement oldTargetModifiedChild, String newContent, RbelJwtFacet jwtFacet) {
        final JsonWebSignature jws = new JsonWebSignature();

        writeHeaderInJws(oldTargetModifiedChild, newContent, jwtFacet, jws);

        jws.setPayload(extractJwsBodyClaims(oldTargetModifiedChild, newContent, jwtFacet));

        jws.setKey(extractJwsKey(jwtFacet, jws));

        if (!jwtFacet.getSignature().getFacetOrFail(RbelJwtSignature.class).isValid()) {
            throw new InvalidJwtSignatureException(
                "The signature is invalid\n" + jwtFacet.getSignature().printTreeStructure());
        }

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new JwtUpdateException("Error writing into Jwt", e);
        }
    }

    private Key extractJwsKey(RbelJwtFacet jwtFacet, JsonWebSignature jsonWebSignature) {
        return jwtFacet.getSignature().getFacet(RbelJwtSignature.class)
            .map(RbelJwtSignature::getVerifiedUsing)
            .filter(obj -> Objects.nonNull(obj))
            .flatMap(verifiedUsing -> verifiedUsing.seekValue(String.class))
            .flatMap(rbelKeyManager::findKeyByName)
            .flatMap(this::getKeyBasedOnEncryptionType)
            .orElseThrow(() -> new InvalidJwtSignatureException(
                "Could not find the key matching signature \n" + jwtFacet.getSignature().printTreeStructure()));
    }

    private void writeHeaderInJws(RbelElement oldTargetModifiedChild, String newContent, RbelJwtFacet jwtFacet,
        JsonWebSignature jws) {
        extractJwtHeaderClaims(oldTargetModifiedChild, newContent, jwtFacet)
            .forEach(pair -> jws.setHeader(pair.getKey(), pair.getValue()));
    }

    private List<Map.Entry<String, String>> extractJwtHeaderClaims(RbelElement oldTargetModifiedChild,
        String newContent,
        RbelJwtFacet jwtFacet) {
        if (jwtFacet.getHeader() == oldTargetModifiedChild) {
            return JsonUtils.convertJsonObjectStringToMap(newContent);
        } else {
            return JsonUtils.convertJsonObjectStringToMap(jwtFacet.getHeader().getRawStringContent());
        }
    }

    private String extractJwsBodyClaims(RbelElement oldTargetModifiedChild, String newContent,
        RbelJwtFacet jwtFacet) {
        if (jwtFacet.getBody() == oldTargetModifiedChild) {
            return newContent;
        } else {
            return jwtFacet.getBody().getRawStringContent();
        }
    }

    private Optional<Key> getKeyBasedOnEncryptionType(RbelKey rbelKey) {
        if (rbelKey.getKey().getAlgorithm().equals("HS256") || rbelKey.getKey().getAlgorithm().equals("HS512") || rbelKey.getKey().getAlgorithm().equals("HS384")) {
            return Optional.ofNullable(rbelKey.getKey());
        } else {
            return rbelKeyManager.findCorrespondingPrivateKey(rbelKey.getKeyName())
                .map(RbelKey::getKey);
        }
    }

    public class JwtUpdateException extends RuntimeException {

        public JwtUpdateException(String s, JoseException e) {
            super(s, e);
        }
    }

    public class InvalidJwtSignatureException extends RuntimeException {

        public InvalidJwtSignatureException(String s) {
            super(s);
        }
    }
}

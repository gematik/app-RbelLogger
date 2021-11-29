package de.gematik.rbellogger.modifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.elements.RbelJwtSignature;
import de.gematik.rbellogger.data.facet.RbelJwtFacet;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.modifier.RbelJwtWriter.InvalidJwtSignatureException;
import de.gematik.rbellogger.modifier.RbelJwtWriter.JwtUpdateException;
import de.gematik.rbellogger.modifier.RbelModifier.RbelModificationException;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.junit.jupiter.api.Test;

public class JwtModifierTest extends AbstractModifierTest {

    @Override
    public RbelConfiguration getRbelConfiguration() {
        return super.getRbelConfiguration()
            .addInitializer(RBEL_KEY_FOLDER_INITIALIZER);
    }

    @Test
    public void modifyJwtHeader_shouldContainModifiedContent() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.header.kid")
            .replaceWith("not the real header")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findElement("$.body").get()
            .hasFacet(RbelJwtFacet.class))
            .isTrue();
        assertThat(modifiedMessage.findElement("$.body.header.kid")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("not the real header");
    }

    @Test
    public void modifyJwtBody_shouldContainModifiedContent() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.body.authorization_endpoint")
            .replaceWith("not the auth endpoint")
            .build());
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.body.token_endpoint")
            .replaceWith("not the token endpoint")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        assertThat(modifiedMessage.findElement("$.body.body.authorization_endpoint")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("not the auth endpoint");
        assertThat(modifiedMessage.findElement("$.body.body.token_endpoint")
            .map(RbelElement::getRawStringContent).get())
            .isEqualTo("not the token endpoint");
    }

    @Test
    public void modifyJwtBody_jwtSignatureVerified() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.body.sso_endpoint")
            .replaceWith("not the original sso endpoint")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);
        final RbelJwtSignature signature = modifiedMessage.findElement("$.body.signature")
            .get().getFacetOrFail(RbelJwtSignature.class);

        assertThat(signature.isValid()).isTrue();
        assertThat(signature.getVerifiedUsing().seekValue(String.class)
            .get()).isEqualTo("puk_idp-fd-sig-refimpl-3");
    }

    @Test
    public void modifyJwtHeader_jwtSignatureVerified() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.header.kid")
            .replaceWith("not the original kid")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);
        final RbelJwtSignature signature = modifiedMessage.findElement("$.body.signature")
            .get().getFacetOrFail(RbelJwtSignature.class);

        assertThat(signature.isValid()).isTrue();
        assertThat(signature.getVerifiedUsing().seekValue(String.class)
            .get()).isEqualTo("puk_idp-fd-sig-refimpl-3");
    }

    @Test
    public void modifyJwtHeader_cantEditAlg() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");
        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.header.alg")
            .replaceWith("ES256")
            .build());

        assertThatThrownBy(() -> modifyMessageAndParseResponse(message))
            .isInstanceOf(JwtUpdateException.class)
            .hasMessageContaining("Error writing into Jwt")
            .hasRootCauseMessage("ES256/SHA256withECDSA expects a key using P-256 but was BP-256");
    }

    @Test
    public void jwtSignature_cantBeRewritten() throws IOException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.signature.verifiedUsing")
            .replaceWith("false verifiedUsing")
            .build());

        assertThatThrownBy(() -> modifyMessageAndParseResponse(message))
            .isInstanceOf(RbelModificationException.class)
            .hasMessageContaining("Could not rewrite element with facets [RbelJwtSignature]");
    }

    @Test
    public void modifyJwt_noMatchingPrivateKeyFound() throws IOException, IllegalAccessException {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");

        setKeyManagerAvailableKeys(rbelLogger.getRbelKeyManager().getAllKeys()
            .filter(key -> key.getKey() instanceof PublicKey)
            .collect(Collectors.toList()));

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.header.kid")
            .replaceWith("not the original kid")
            .build());

        assertThatThrownBy(() -> modifyMessageAndParseResponse(message))
            .isInstanceOf(InvalidJwtSignatureException.class)
            .hasMessageContaining("Could not find the key matching signature");
    }

    @Test
    public void modifyJwt_noPublicAndPrivateKeysFound() throws Exception {
        final RbelElement message = readAndConvertCurlMessage("src/test/resources/sampleMessages/jwtMessage.curl");
        setKeyManagerAvailableKeys(new ArrayList<>());

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.header.kid")
            .replaceWith("not the original kid")
            .build());

        assertThatThrownBy(() -> modifyMessageAndParseResponse(message))
            .isInstanceOf(InvalidJwtSignatureException.class)
            .hasMessageContaining("Could not find the key matching signature");
    }

    @Test
    public void modifyJwt_falseSecret() throws IOException {
        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/jwtMessageWithFalseSecret.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.body.name")
            .replaceWith("other name")
            .build());

        assertThatThrownBy(() -> modifyMessageAndParseResponse(message))
            .isInstanceOf(InvalidJwtSignatureException.class)
            .hasMessageContaining("Could not find the key matching signature");
    }

    @Test
    public void modifyJwt_correctSecret() throws IOException {
        RbelKey secretKey = RbelKey.builder()
            .keyName("secretKey")
            .key(new SecretKeySpec(("n2r5u8x/A?D(G-KaPdSgVkYp3s6v9y$B").getBytes("UTF-8"), AlgorithmIdentifiers.HMAC_SHA256))
            .build();
        rbelLogger.getRbelKeyManager().addKey(secretKey);

        final RbelElement message = readAndConvertCurlMessage(
            "src/test/resources/sampleMessages/jwtMessageWithSecret.curl");

        rbelLogger.getRbelModifier().addModification(RbelModificationDescription.builder()
            .targetElement("$.body.body.name")
            .replaceWith("other name")
            .build());

        final RbelElement modifiedMessage = modifyMessageAndParseResponse(message);

        final RbelJwtSignature signature = modifiedMessage.findElement("$.body.signature")
            .get().getFacetOrFail(RbelJwtSignature.class);

        assertThat(signature.isValid()).isTrue();
    }
}

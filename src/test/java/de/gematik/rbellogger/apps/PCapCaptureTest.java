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

package de.gematik.rbellogger.apps;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.*;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelMarkdownRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Key;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class PCapCaptureTest {
    private final static Map<String, String> MASKING_FUNCTIONS = new HashMap<>();

    {
        MASKING_FUNCTIONS.put("exp", "[Gültigkeit des Tokens. Beispiel: %s]");
        MASKING_FUNCTIONS
            .put("iat", "[Zeitpunkt der Ausstellung des Tokens. Beispiel: %s]");
        MASKING_FUNCTIONS
            .put("nbf",
                "[Der Token ist erst ab diesem Zeitpunkt gültig. Beispiel: %s]");

        MASKING_FUNCTIONS.put("code_challenge",
            "[code_challenge value, Base64URL(SHA256(code_verifier)). Beispiel: %s]");

        MASKING_FUNCTIONS.put("nonce", "[String value used to associate a Client session with an ID Token, and to mitigate replay attacks. Beispiel: %s]");

        MASKING_FUNCTIONS.put("state","[OAuth 2.0 state value. Constant over complete flow. Value is a case-sensitive string. Beispiel: %s]");

        MASKING_FUNCTIONS.put("jti", "[A unique identifier for the token, which can be used to prevent reuse of the token. Value is a case-sensitive string. Beispiel: %s]");

        MASKING_FUNCTIONS.put("given_name",
            "[givenName aus dem subject-DN des authentication-Zertifikats. Beispiel: %s]");
        MASKING_FUNCTIONS.put("family_name",
            "[surname aus dem subject-DN des authentication-Zertifikats. Beispiel: %s]");
        MASKING_FUNCTIONS.put("idNummer",
            "[KVNR oder Telematik-ID aus dem authentication-Zertifikats. Beispiel: %s]");
        MASKING_FUNCTIONS.put("professionOID",
            "[professionOID des HBA aus dem authentication-Zertifikats. Null if not present. Beispiel: %s]");
        MASKING_FUNCTIONS.put("organizationName",
            "[professionOID des HBA  aus dem authentication-Zertifikats. Null if not present. Beispiel: %s]");
        MASKING_FUNCTIONS.put("auth_time", "[timestamp of authentication. Technically this is the time of authentication-token signing. Beispiel: %s]");
        MASKING_FUNCTIONS.put("snc",
            "[server-nonce. Used to introduce noise. Beispiel: %s]");
//        MASKING_FUNCTIONS.put("cnf",
//            "[confirmation. Authenticated certificate of the client. For details see rfc7800. Beispiel: " +
//                v.toString(), " ".repeat(60)) + "]");
        MASKING_FUNCTIONS.put("sub",
            "[subject. Base64(sha256(audClaim + idNummerClaim + serverSubjectSalt)). Beispiel: %s]");
        MASKING_FUNCTIONS.put("at_hash",
                "[Erste 16 Bytes des Hash des Authentication Tokens Base64(subarray(Sha256(authentication_token), 0, 16)). Beispiel: %s]");
 //       MASKING_FUNCTIONS.put("x5c",
 //           "[Enthält das verwendete Signer-Zertifikat. Beispiel: " + prettyPrintJsonString(v.toString(),
 //               " ".repeat(60)) + "]");

        MASKING_FUNCTIONS.put("authorization_endpoint", "[URL des Authorization Endpunkts.]");
        MASKING_FUNCTIONS.put("sso_endpoint", "[URL des Authorization Endpunkts.]");
        MASKING_FUNCTIONS.put("token_endpoint", "[URL des Authorization Endpunkts.]");
        MASKING_FUNCTIONS.put("uri_disc", "[URL des Discovery-Dokuments]");
        MASKING_FUNCTIONS.put("puk_uri_auth", "[URL einer JWK-Struktur des Authorization Public-Keys]");
        MASKING_FUNCTIONS.put("puk_uri_token", "[URL einer JWK-Struktur des Token Public-Keys]");
        MASKING_FUNCTIONS.put("jwks_uri", "[URL einer JWKS-Struktur mit allen vom Server verwendeten Schlüsseln]");
        MASKING_FUNCTIONS.put("njwt", "[enthält das Ursprüngliche Challenge Token des Authorization Endpunkt]");
        MASKING_FUNCTIONS.put("Date", "[Zeitpunkt der Antwort. Beispiel %s]");
    }

    private static final BiConsumer<RbelElement, RbelConverter> RBEL_IDP_TOKEN_KEY_LISTENER = (element, converter) ->
        Optional.ofNullable(((RbelJweElement) element).getBody())
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
            .ifPresent(aesKey -> converter.getKeyIdToKeyDatabase().put("token_key", aesKey));

    @Test
    @Disabled
    public void listAllDevices() {
        PCapCapture.builder()
            .listDevices(true)
            .build()
            .run();
    }

    @Test
    @Disabled
    public void pcapFileDump() {
        PCapCapture.builder()
            .pcapFile("src/test/resources/discDoc.pcap")
            .printMessageToSystemOut(false)
            .rbel(RbelConverter.build(new RbelConfiguration()
                .addPostConversionListener(RbelHttpResponse.class,
                    (el, context) -> System.out.println(el))))
            .build()
            .run();
    }

    @Test
    @Disabled
    public void forceCleanContent() throws IOException {
        final RbelConverter rbelConverter = RbelConverter.build(new RbelConfiguration()
            .addKey("IDP symmetricEncryptionKey",
                new SecretKeySpec(DigestUtils.sha256("geheimerSchluesselDerNochGehashtWird"), "AES"))
            .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
            .addPostConversionListener(RbelJweElement.class, RBEL_IDP_TOKEN_KEY_LISTENER));
        PCapCapture.builder()
            .pcapFile("src/test/resources/pairingList.pcap")
            .printMessageToSystemOut(false)
            .rbel(rbelConverter)
            .build()
            .run();
        rbelConverter.getMessageHistory().stream()
            .forEach(el -> System.out.println(RbelMarkdownRenderer.render(el)));

        FileUtils.writeStringToFile(new File("target/PairingDelete.html"),
            RbelHtmlRenderer
                .render(rbelConverter.getMessageHistory(), new RbelValueShader(MASKING_FUNCTIONS)), Charset.defaultCharset());

        assertThat(rbelConverter.getMessageHistory().get(0))
            .isInstanceOf(RbelHttpRequest.class);
        assertThat(rbelConverter.getMessageHistory().get(1))
            .isInstanceOf(RbelHttpResponse.class);
    }
    /* How to print from cmdline

    t.eitzenberger@GNDEV038 MINGW64 ~/IdeaProjects/rbel-logger/target (master)
    $ /c/Program\ Files/Google/Chrome/Application/chrome.exe --headless -disable-gpu C:/Users/t.eitzenberger/IdeaProjects/rbel-logger/target/pairingList.html  --print-to-pdf=C:/temp/output.pdf

     */
}

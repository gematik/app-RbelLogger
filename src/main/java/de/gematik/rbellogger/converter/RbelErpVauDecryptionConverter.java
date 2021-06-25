package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.converter.brainpool.BrainpoolCurves;
import de.gematik.rbellogger.data.elements.RbelBinaryElement;
import de.gematik.rbellogger.data.elements.RbelElement;
import de.gematik.rbellogger.data.elements.RbelNestedElement;
import de.gematik.rbellogger.data.elements.RbelVauErpMessage;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.util.CryptoUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import wiremock.org.apache.commons.codec.binary.Hex;

@Slf4j
public class RbelErpVauDecryptionConverter implements RbelConverterPlugin {

    private Optional<byte[]> decrypt(byte[] encMessage, ECPrivateKey secretKey) {
        try {
            if (encMessage[0] != 1) {
                return Optional.empty();
            }
            ECPublicKey otherSidePublicKey = extractPublicKeyFromVauMessage(encMessage);
            byte[] sharedSecret = CryptoUtils.ecka(secretKey, otherSidePublicKey);
            byte[] aesKeyBytes = CryptoUtils.hkdf(sharedSecret, "ecies-vau-transport", 16);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            return CryptoUtils.decrypt(Arrays.copyOfRange(encMessage, 1 + 32 + 32, encMessage.length), aesKey);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private ECPublicKey extractPublicKeyFromVauMessage(byte[] encMessage)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        final java.security.spec.ECPoint ecPoint = new java.security.spec.ECPoint(
            new BigInteger(1, Arrays.copyOfRange(encMessage, 1, 1 + 32)),
            new BigInteger(1, Arrays.copyOfRange(encMessage, 1 + 32, 1 + 32 + 32)));
        final ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, BrainpoolCurves.BP256);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);
    }

    private Optional<RbelVauErpMessage> decipherVauMessage(RbelElement element, RbelConverter converter) {
        byte[] content = getBinaryContent(element);
        final List<RbelKey> potentialVauKeys = converter.getRbelKeyManager().getAllKeys()
            .filter(key -> key.getKey() instanceof ECPrivateKey
                || key.getKey() instanceof SecretKey)
            .collect(Collectors.toList());
        for (RbelKey rbelKey : potentialVauKeys) {
            final Optional<byte[]> decryptedBytes = decrypt(content, rbelKey.getKey());
            if (decryptedBytes.isPresent()) {
                try {
                    log.trace("Succesfully deciphered VAU message! ({})", new String(decryptedBytes.get()));
                    if (isVauResponse(decryptedBytes)) {
                        return buildVauMessageFromCleartextResponse(converter, decryptedBytes.get(), content,
                            rbelKey.getKeyName());
                    } else {
                        return buildVauMessageFromCleartextRequest(converter, decryptedBytes.get(), content,
                            rbelKey.getKeyName());
                    }
                } catch (RuntimeException e) {
                    log.error("Exception while deciphering VAU message:", e);
                    throw e;
                }
            }
        }
        return Optional.empty();
    }

    private boolean isVauResponse(Optional<byte[]> decryptedBytes) {
        return decryptedBytes
            .map(bytes -> new String(bytes, StandardCharsets.US_ASCII))
            .map(s -> s.split("1 [\\da-f]{32} ").length)
            .map(length -> length > 1)
            .orElse(false);
    }

    private Optional<byte[]> decrypt(byte[] content, Key key) {
        if (key instanceof ECPrivateKey) {
            return decrypt(content, (ECPrivateKey) key);
        } else if (key instanceof SecretKey) {
            return CryptoUtils.decrypt(content, key, 96 / 8, 128 / 8);
        } else {
            throw new RuntimeException("Unexpected key-type encountered (" + key.getClass().getSimpleName() + ")");
        }
    }

    private Optional<RbelVauErpMessage> buildVauMessageFromCleartextRequest(RbelConverter converter, byte[] decryptedBytes,
        byte[] encryptedMessage, String keyName) {
        String[] vauMessageParts = new String(decryptedBytes).split(" ", 5);
        final SecretKeySpec responseKey = buildAesKeyFromHex(vauMessageParts[3]);
        converter.getRbelKeyManager().addKey("VAU Response-Key", responseKey, 0);
        return Optional.of(RbelVauErpMessage.builder()
            .message(converter.convertElement(vauMessageParts[4]))
            .encryptedMessage(encryptedMessage)
            .requestId(vauMessageParts[2])
            .pVersionNumber(Integer.parseInt(vauMessageParts[0]))
            .responseKey(responseKey)
            .keyIdUsed(keyName)
            .build());
    }

    private Optional<RbelVauErpMessage> buildVauMessageFromCleartextResponse(RbelConverter converter, byte[] decryptedBytes,
        byte[] encryptedMessage, String keyName) {
        String[] vauMessageParts = new String(decryptedBytes).split(" ", 3);
        return Optional.of(RbelVauErpMessage.builder()
            .message(converter.convertElement(vauMessageParts[2]))
            .encryptedMessage(encryptedMessage)
            .requestId(vauMessageParts[1])
            .pVersionNumber(Integer.parseInt(vauMessageParts[0]))
            .keyIdUsed(keyName)
            .build());
    }

    private SecretKeySpec buildAesKeyFromHex(String hexEncodedKey) {
        try {
            return new SecretKeySpec(Hex.decodeHex(hexEncodedKey), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Error during Key decoding", e);
        }
    }

    @Override
    public boolean canConvertElement(RbelElement element, RbelConverter context) {
        return decipherVauMessage(element, context)
            .isPresent();
    }

    @Override
    public RbelElement convertElement(RbelElement element, RbelConverter context) {
        log.trace("Trying to decipher '{}'...", element.getContent());
        final Optional<RbelVauErpMessage> rbelVauMessage = decipherVauMessage(element, context);
        if (rbelVauMessage.isEmpty()) {
            return element;
        }
        if (element instanceof RbelNestedElement) {
            ((RbelNestedElement) element).setNestedElement(rbelVauMessage.get());
            return element;
        } else {
            return rbelVauMessage.get();
        }
    }

    private byte[] getBinaryContent(RbelElement element) {
        if (element instanceof RbelBinaryElement) {
            return ((RbelBinaryElement) element).getRawData();
        }
        return Base64.getDecoder().decode(element.getContent());
    }
}

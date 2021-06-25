package de.gematik.rbellogger.converter;

import static de.gematik.rbellogger.util.CryptoUtils.decrypt;

import de.gematik.rbellogger.data.elements.*;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.util.CryptoUtils;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import wiremock.org.apache.commons.codec.binary.Hex;

@Slf4j
public class RbelVauDecryptionConverter implements RbelConverterPlugin {

    private static RbelVauEpaMessage fromRaw(Pair<byte[], byte[]> payloadPair, RbelConverter converter,
        byte[] decryptedBytes) {
        byte[] raw = new byte[decryptedBytes.length - 8 - 1];
        System.arraycopy(decryptedBytes, 8 + 1, raw, 0, raw.length);

        byte[] sequenceNumberBytes = new byte[4];
        System.arraycopy(decryptedBytes, 5, sequenceNumberBytes, 0, 4);
        int sequenceNumber = java.nio.ByteBuffer.wrap(sequenceNumberBytes).getInt();

        byte[] numberOfBytes_inBytes = new byte[4];
        System.arraycopy(raw, 0, numberOfBytes_inBytes, 0, 4);
        int numberOfBytes = java.nio.ByteBuffer.wrap(numberOfBytes_inBytes).getInt();

        byte[] headerField_inBytes = new byte[numberOfBytes];
        System.arraycopy(raw, 4, headerField_inBytes, 0, numberOfBytes);
        String headerField = new String(headerField_inBytes, StandardCharsets.US_ASCII);

        RbelMultiValuedMapElement header = new RbelMultiValuedMapElement();
        Arrays.stream(headerField.split("\r\n"))
            .map(field -> field.split(":", 2))
            .forEach(field -> header.put(field[0].trim(), converter.convertElement(field[1])));

        byte[] body = new byte[raw.length - 4 - numberOfBytes];
        System.arraycopy(raw, 4 + numberOfBytes, body, 0, body.length);

        return RbelVauEpaMessage.builder()
            .message(converter.convertElement(body))
            .additionalHeaders(header)
            .encryptedMessage(payloadPair.getValue())
            .keyIdUsed(Hex.encodeHexString(payloadPair.getKey()))
            .pVersionNumber((int) decryptedBytes[0])
            .sequenceNumber((long) sequenceNumber)
            .build();
    }

    public static Pair<byte[], byte[]> splitVauMessage(byte[] vauMessage) {
        byte[] keyID = new byte[32];
        System.arraycopy(vauMessage, 0, keyID, 0, 32);
        byte[] enc = new byte[vauMessage.length - 32];
        System.arraycopy(vauMessage, 32, enc, 0, vauMessage.length - 32);
        return Pair.of(keyID, enc);
    }

    private Optional<RbelVauEpaMessage> decipherVauMessage(byte[] content, RbelConverter converter) {
        final Pair<byte[], byte[]> splitVauMessage = splitVauMessage(content);
        final List<RbelKey> potentialVauKeys = converter.getRbelKeyManager().getAllKeys()
            .filter(key -> key.getKeyName().startsWith(Hex.encodeHexString(splitVauMessage.getKey())))
            .filter(key -> key.getKey() instanceof SecretKey)
            .collect(Collectors.toList());
        for (RbelKey rbelKey : potentialVauKeys) {
            Optional<byte[]> decryptedBytes = decrypt(splitVauMessage.getValue(), rbelKey.getKey(),
                CryptoUtils.GCM_IV_LENGTH_IN_BYTES, CryptoUtils.GCM_TAG_LENGTH_IN_BYTES);
            if (decryptedBytes.isPresent()) {
                try {
                    log.trace("Succesfully deciphered VAU message! ({})", new String(decryptedBytes.get()));
                    return buildVauMessageFromCleartext(converter, splitVauMessage, decryptedBytes.get());
                } catch (RuntimeException e) {
                    log.error("Exception while deciphering VAU message:", e);
                    throw e;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RbelVauEpaMessage> buildVauMessageFromCleartext(RbelConverter converter,
        Pair<byte[], byte[]> splitVauMessage,
        byte[] decryptedBytes) {
        final String cleartextString = new String(decryptedBytes);
        if (cleartextString.startsWith("VAUClientSigFin")
            || cleartextString.startsWith("VAUServerFin")) {
            return Optional.of(RbelVauEpaMessage.builder()
                .message(converter.convertElement(new String(decryptedBytes)))
                .encryptedMessage(splitVauMessage.getValue())
                .keyIdUsed(Hex.encodeHexString(splitVauMessage.getKey()))
                .build());
        } else {
            return Optional.of(fromRaw(splitVauMessage, converter, decryptedBytes));
        }
    }

    @Override
    public boolean canConvertElement(RbelElement element, RbelConverter context) {
        return decipherVauMessage(getBinaryContent(element), context)
            .isPresent();
    }

    @Override
    public RbelElement convertElement(RbelElement element, RbelConverter context) {
        log.trace("Trying to decipher '{}'...", element.getContent());
        final Optional<RbelVauEpaMessage> rbelVauMessage = decipherVauMessage(getBinaryContent(element), context);
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

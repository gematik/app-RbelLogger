package de.gematik.rbellogger.data.elements;

import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
public class RbelVauErpMessage extends RbelGenericVauMessage {

    private final String requestId;
    private final SecretKey responseKey;

    @Builder
    public RbelVauErpMessage(RbelElement message, byte[] encryptedMessage, String keyIdUsed,
        Integer pVersionNumber, String requestId, SecretKey responseKey) {
        super(message, encryptedMessage, keyIdUsed, pVersionNumber);
        this.requestId = requestId;
        this.responseKey = responseKey;
    }

    @Override
    public List<? extends RbelElement> getChildNodes() {
        return List.of(getMessage());
    }

    @Override
    public String getContent() {
        return Base64.getEncoder().encodeToString(getEncryptedMessage());
    }
}

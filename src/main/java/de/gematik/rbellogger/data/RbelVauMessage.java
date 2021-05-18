package de.gematik.rbellogger.data;

import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RbelVauMessage extends RbelElement {

    private final RbelElement message;
    private final RbelVauProtocolType rbelVauProtocolType;
    private final String keyIdUsed;
    private final Integer pVersionNumber;
    private final Long sequenceNumber;
    private final String requestId;
    private final byte[] encryptedMessage;
    private final SecretKey responseKey;
    private final RbelMultiValuedMapElement additionalHeaders;

    @Override
    public List<? extends RbelElement> getChildNodes() {
        return List.of(message);
    }

    @Override
    public String getContent() {
        return Base64.getEncoder().encodeToString(encryptedMessage);
    }
}

package de.gematik.rbellogger.data.elements;

import java.util.Base64;
import java.util.List;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
public class RbelVauEpaMessage extends RbelGenericVauMessage {

    private final RbelMultiValuedMapElement additionalHeaders;
    private final Long sequenceNumber;

    @Builder
    public RbelVauEpaMessage(RbelElement message, byte[] encryptedMessage, String keyIdUsed,
        Integer pVersionNumber, RbelMultiValuedMapElement additionalHeaders, Long sequenceNumber) {
        super(message, encryptedMessage, keyIdUsed, pVersionNumber);
        this.additionalHeaders = additionalHeaders;
        this.sequenceNumber = sequenceNumber;
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

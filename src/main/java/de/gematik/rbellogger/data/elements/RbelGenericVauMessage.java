package de.gematik.rbellogger.data.elements;

import java.util.Base64;
import java.util.List;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
public class RbelGenericVauMessage extends RbelElement {

    private final RbelElement message;
    private final byte[] encryptedMessage;
    private final String keyIdUsed;
    private final Integer pVersionNumber;

    @Override
    public List<? extends RbelElement> getChildNodes() {
        return List.of(message);
    }

    @Override
    public String getContent() {
        return Base64.getEncoder().encodeToString(encryptedMessage);
    }
}

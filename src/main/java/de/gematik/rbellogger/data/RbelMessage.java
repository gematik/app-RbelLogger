package de.gematik.rbellogger.data;

import de.gematik.rbellogger.data.elements.RbelElement;
import de.gematik.rbellogger.data.elements.RbelHttpMessage;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RbelMessage {

    private final RbelHttpMessage httpMessage;
    private final long sequenceNumber;
    private final RbelHostname sender;
    private final RbelHostname recipient;
    private final String uuid = UUID.randomUUID().toString();

    public String getContent() {
        return httpMessage.getContent();
    }

    public List<RbelElement> findRbelPathMembers(String rbelPathExpression) {
        return httpMessage.findRbelPathMembers(rbelPathExpression);
    }

    public String getNote() {
        return httpMessage.getNote();
    }
}

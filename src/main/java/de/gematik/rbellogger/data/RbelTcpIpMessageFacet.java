package de.gematik.rbellogger.data;

import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.data.facet.RbelHostnameFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHttpMessageRenderer;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class RbelTcpIpMessageFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHttpMessageRenderer());
    }

    private final long sequenceNumber;
    private final RbelElement sender;
    private final RbelElement receiver;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("sender", sender),
            Pair.of("receiver", receiver)
        );
    }

    public RbelHostname getSenderHostname() {
        return sender.getFacetOrFail(RbelHostnameFacet.class).toRbelHostname();
    }

    public RbelHostname getReceiverHostname() {
        return receiver.getFacetOrFail(RbelHostnameFacet.class).toRbelHostname();
    }
}

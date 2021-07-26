package de.gematik.rbellogger.data;

import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHttpMessageRenderer;
import java.util.List;
import java.util.Map.Entry;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
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
            Pair.of("recipient", receiver)
        );
    }

    public RbelHostname getSenderHostname() {
        return sender.seekValue(RbelHostname.class).orElseThrow();
    }

    public RbelHostname getReceiverHostname() {
        return receiver.seekValue(RbelHostname.class).orElseThrow();
    }
}

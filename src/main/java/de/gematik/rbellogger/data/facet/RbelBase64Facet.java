package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
public class RbelBase64Facet implements RbelFacet {

    private final RbelElement child;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("decoded", child)
        );
    }
}

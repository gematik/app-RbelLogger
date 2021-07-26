package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
public class RbelAsn1TaggedValueFacet implements RbelFacet {

    private final RbelElement tag;
    private final RbelElement nestedElement;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        if (nestedElement == null) {
            return List.of();
        } else {
            return List.of(
                Pair.of("content", nestedElement),
                Pair.of("tag", tag)
            );
        }
    }
}

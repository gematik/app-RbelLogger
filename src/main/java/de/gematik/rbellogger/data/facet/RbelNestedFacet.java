package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@Builder
@RequiredArgsConstructor
public class RbelNestedFacet implements RbelFacet {

    private final RbelElement nestedElement;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("content", nestedElement)
        );
    }
}

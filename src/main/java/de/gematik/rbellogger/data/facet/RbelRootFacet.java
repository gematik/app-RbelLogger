package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class RbelRootFacet<T extends RbelFacet> implements RbelFacet {

    private final T rootFacet;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of();
    }
}

package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Builder
@Data
public class RbelUriParameterFacet implements RbelFacet {

    private final RbelElement key;
    private final RbelElement value;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("key", key),
            Pair.of("value", value)
        );
    }

    public String getKeyAsString() {
        return key.seekValue(String.class)
            .orElseThrow();
    }
}

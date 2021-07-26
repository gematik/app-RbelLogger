package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class RbelBearerTokenFacet implements RbelFacet {

    private final RbelElement bearerToken;

    @Override
    public List<Map.Entry<String, RbelElement>> getChildElements() {
        return List.of(
                Pair.of("BearerToken", bearerToken)
        );
    }
}

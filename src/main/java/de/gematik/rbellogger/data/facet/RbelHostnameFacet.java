package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class RbelHostnameFacet implements RbelFacet {

    private final RbelElement port;
    private final RbelElement domain;
    private final Optional<RbelElement> bundledServerName;

    public static RbelElement buildRbelHostnameFacet(RbelElement parentNode, RbelHostname rbelHostname) {
        return buildRbelHostnameFacet(parentNode, rbelHostname, null);
    }

    public static RbelElement buildRbelHostnameFacet(RbelElement parentNode, RbelHostname rbelHostname, String bundledServerName) {
        if (rbelHostname == null) {
            return new RbelElement(null, parentNode);
        }
        final RbelElement result = new RbelElement(rbelHostname.toString().getBytes(StandardCharsets.UTF_8), parentNode);
        result.addFacet(RbelHostnameFacet.builder()
            .port(RbelElement.wrap(result, rbelHostname.getPort()))
            .domain(RbelElement.wrap(result, rbelHostname.getHostname()))
            .bundledServerName(Optional.ofNullable(bundledServerName)
                .map(bundledName -> RbelElement.wrap(parentNode, bundledName)))
            .build());
        return result;
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("port", port),
            Pair.of("domain", domain)
        );
    }

    public String toString() {
        return bundledServerName
            .flatMap(el -> el.seekValue(String.class))
            .or(() -> domain.seekValue(String.class))
            .orElseThrow(() -> new RbelHostnameStructureException("Could not find domain-name!"))
            + port.seekValue(Integer.class)
            .map(port -> ":" + port).orElse("");
    }

    public RbelHostname toRbelHostname() {
        return RbelHostname.builder()
            .hostname(bundledServerName
                .flatMap(el -> el.seekValue(String.class))
                .or(() -> domain.seekValue(String.class))
                .orElseThrow())
            .port(port.seekValue(Integer.class).orElse(0))
            .build();
    }

    private class RbelHostnameStructureException extends RuntimeException {
        public RbelHostnameStructureException(String s) {
            super(s);
        }
    }
}

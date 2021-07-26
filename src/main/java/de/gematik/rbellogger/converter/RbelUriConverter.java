package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelUriFacet;
import de.gematik.rbellogger.data.facet.RbelUriFacet.RbelUriFacetBuilder;
import de.gematik.rbellogger.data.facet.RbelUriParameterFacet;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class RbelUriConverter implements RbelConverterPlugin {

    public List<RbelElement> extractParameterMap(final URI uri, final RbelConverter context,
        String originalContent, RbelElement parentNode) {
        if (uri.getQuery() == null) {
            return List.of();
        }

        final Map<String, String> rawStringMap = Stream.of(originalContent.split("\\?")[1].split("\\&"))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toMap(param -> param.split("\\=")[0], Function.identity()));

        return Stream.of(uri.getQuery().split("&"))
            .filter(param -> param.contains("="))
            .map(param -> param.split("=", 2))
            .map(array -> {
                    RbelElement paramPair = new RbelElement(rawStringMap.get(array[0]).getBytes(), parentNode);
                    paramPair.addFacet(RbelUriParameterFacet.builder()
                        .key(RbelElement.wrap(paramPair, array[0]))
                        .value(context.convertElement(array[1].getBytes(), paramPair))
                        .build());
                    return paramPair;
                }
            )
            .collect(Collectors.toList());
    }

    public boolean canConvertElement(final RbelElement rbel) {
        try {
            final URI uri = new URI(rbel.getRawStringContent());
            final boolean hasQuery = uri.getQuery() != null;
            final boolean hasProtocol = uri.getScheme() != null;
            return hasQuery || hasProtocol || rbel.getRawStringContent().startsWith("/");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public void consumeElement(final RbelElement rbel, final RbelConverter context) {
        if (!canConvertElement(rbel)) {
            return;
        }
        final URI uri = convertToUri(rbel);

        final String[] pathParts = rbel.getRawStringContent().split("\\?", 2);
        final RbelUriFacetBuilder uriFacetBuilder = RbelUriFacet.builder()
            .basicPath(RbelElement.wrap(rbel, pathParts[0]));
        if (pathParts.length > 1) {
            uriFacetBuilder.queryParameters(extractParameterMap(uri, context, rbel.getRawStringContent(), rbel));
        } else {
            uriFacetBuilder.queryParameters(List.of());
        }
        rbel.addFacet(uriFacetBuilder.build());
    }

    private URI convertToUri(RbelElement target) {
        try {
            return new URI(target.getRawStringContent());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to convert Path-Element", e);
        }
    }
}

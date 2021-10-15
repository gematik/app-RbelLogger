package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.UnescapedText;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;

@Builder
@Data
public class RbelUriFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHtmlFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelUriFacet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                RbelHtmlRenderingToolkit renderingToolkit) {
                final RbelUriFacet uriFacet = element.getFacetOrFail(RbelUriFacet.class);
                final String originalUrl = element.getRawStringContent();
                final ContainerTag urlContent = renderUrlContent(renderingToolkit, uriFacet, originalUrl);
                final DomContent note = addNote(element);
                if (element.traverseAndReturnNestedMembers().isEmpty()) {
                    return div().with(urlContent).with(note);
                } else {
                    return ancestorTitle().with(
                            vertParentTitle().with(
                                    div().withClass("tile is-child pr-3")
                                            .with(urlContent)
                                            .with(note)
                                            .with(renderingToolkit.convertNested(element))));
                }            }

            private ContainerTag renderUrlContent(RbelHtmlRenderingToolkit renderingToolkit, RbelUriFacet uriFacet, String originalUrl) {
                if (!originalUrl.contains("?")) {
                    return div(new UnescapedText(originalUrl));
                } else {
                    final ContainerTag div = div(uriFacet.getBasicPathString() + "?").with(br());
                    boolean firstElement = true;
                    for (final RbelElement queryElementEntry : uriFacet.getQueryParameters()) {
                        final RbelUriParameterFacet parameterFacet = queryElementEntry
                                .getFacetOrFail(RbelUriParameterFacet.class);
                        final String shadedStringContent = renderingToolkit
                                .shadeValue(parameterFacet.getValue(), Optional.of(parameterFacet.getKeyAsString()))
                                .map(content -> queryElementEntry.getKey() + "=" + content)
                                .orElse(queryElementEntry.getRawStringContent());

                        div.with(div((firstElement ? "" : "&") + shadedStringContent)
                                .with(addNote(queryElementEntry, " ml-6")));
                        firstElement = false;
                    }
                    return div;
                }
            }
        });
    }

    private final RbelElement basicPath;
    private final List<RbelElement> queryParameters;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        List<Entry<String, RbelElement>> result = new ArrayList();
        result.addAll(queryParameters.stream()
            .map(el -> Pair.of(el.getFacetOrFail(RbelUriParameterFacet.class).getKeyAsString(), el))
            .collect(Collectors.toList())
        );
        result.add(Pair.of("basicPath", basicPath));
        return result;
    }

    public String getBasicPathString() {
        return basicPath.seekValue(String.class)
            .orElseThrow();
    }

    public Optional<RbelElement> getQueryParameter(String key) {
        Objects.requireNonNull(key);
        return queryParameters.stream()
            .map(element -> element.getFacetOrFail(RbelUriParameterFacet.class))
            .filter(e -> e.getKeyAsString().equals(key))
            .map(RbelUriParameterFacet::getValue)
            .findFirst();
    }
}

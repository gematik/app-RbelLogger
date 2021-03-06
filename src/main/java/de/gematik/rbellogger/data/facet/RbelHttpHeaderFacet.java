package de.gematik.rbellogger.data.facet;

import static j2html.TagCreator.*;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public class RbelHttpHeaderFacet implements RbelFacet, Map<String, RbelElement> {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelHttpHeaderFacet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                RbelHtmlRenderingToolkit renderingToolkit) {
                return table()
                    .withClass("table").with(
                        thead(
                            tr(th("name"), th("value"))
                        ),
                        tbody().with(
                            element.getFacetOrFail(RbelHttpHeaderFacet.class).getChildElements().stream()
                                .map(entry ->
                                    tr(
                                        td(pre(entry.getKey())),
                                        td(pre()
                                            .with(renderingToolkit.convert(entry.getValue(), Optional.ofNullable(entry.getKey())))
                                            .withClass("value"))
                                            .with(renderingToolkit.addNote(entry.getValue()))
                                    )
                                )
                                .collect(Collectors.toList())
                        )
                    );
            }
        });
    }

    private final Map<String, List<RbelElement>> values;

    public RbelHttpHeaderFacet() {
        this.values = new LinkedHashMap<>();
    }

    public RbelHttpHeaderFacet(Map<String, List<RbelElement>> values) {
        this.values = values;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsKey(value);
    }

    @Override
    public RbelElement get(Object key) {
        final List<RbelElement> list = values.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public RbelElement put(String key, RbelElement value) {
        if (!values.containsKey(key)) {
            values.put(key, new ArrayList<>());
        }
        values.get(key).add(value);
        return value;
    }

    @Override
    public RbelElement remove(Object key) {
        final List<RbelElement> list = values.remove(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public void putAll(Map<? extends String, ? extends RbelElement> m) {
        for (Entry<? extends String, ? extends RbelElement> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public List<RbelElement> values() {
        return values.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, RbelElement>> entrySet() {
        final HashSet<Entry<String, RbelElement>> result = new HashSet<>();
        result.addAll(getChildElements());
        return result;
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return values.entrySet().stream()
            .flatMap(entry -> {
                if (entry.getValue().isEmpty()) {
                    return Stream.of();
                } else {
                    return entry.getValue().stream()
                        .map(value -> Pair.of(entry.getKey(), value));
                }
            })
            .collect(Collectors.toList());
    }

    public Stream<RbelElement> getCaseInsensitiveMatches(String key) {
        final String lowerCaseKey = key.toLowerCase();
        return values.entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .filter(entry -> entry.getKey().toLowerCase().equals(lowerCaseKey))
            .map(Entry::getValue)
            .flatMap(List::stream);
    }

    public boolean hasValueMatching(String headerKey, String prefix) {
        return values.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(headerKey))
            .map(Entry::getValue)
            .flatMap(List::stream)
            .map(RbelElement::getRawStringContent)
            .anyMatch(str -> str.startsWith(prefix));
    }
}

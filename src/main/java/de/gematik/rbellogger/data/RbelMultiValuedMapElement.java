package de.gematik.rbellogger.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public class RbelMultiValuedMapElement extends RbelElement implements Map<String, RbelElement> {

    private final Map<String, List<RbelElement>> values;

    public RbelMultiValuedMapElement(Map<String, List<RbelElement>> values) {
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
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public RbelElement put(String key, RbelElement value) {
        if (!values.containsKey(key)) {
            values.get(key).add(value);
        }
        return value;
    }

    @Override
    public RbelElement remove(Object key) {
        final List<RbelElement> list = values.remove(key);
        if (list.isEmpty()) {
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
    public Collection<RbelElement> values() {
        return values.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, RbelElement>> entrySet() {
        return values.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(value -> Pair.of(entry.getKey(), value)))
            .collect(Collectors.toSet());
    }

    @Override
    public List<RbelElement> getChildNodes() {
        return values.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public String getContent() {
        return getRawMessage();
    }

    @Override
    public Set<Entry<String, RbelElement>> getChildElements() {
        return values.entrySet().stream()
            .flatMap(entry -> {
                if (entry.getValue().isEmpty()) {
                    return Stream.of();
                } else {
                    return entry.getValue().stream()
                        .map(value -> Pair.of(entry.getKey(), value));
                }
            })
            .collect(Collectors.toSet());
    }
}
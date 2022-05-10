/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.data;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class RbelMultiMap implements Map<String, RbelElement> {

    public static final Collector<Entry<String, RbelElement>, RbelMultiMap, RbelMultiMap> COLLECTOR =
        Collector.of(RbelMultiMap::new, RbelMultiMap::put, (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        });

    private final List<Map.Entry<String, RbelElement>> values = new ArrayList();

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
        return values.stream()
            .anyMatch(entry -> entry.getKey().equals(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return values.stream()
            .anyMatch(entry -> entry.getValue().equals(value));
    }

    @Override
    public RbelElement get(Object key) {
        return values.stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .findFirst().orElse(null);
    }

    @Override
    public RbelElement put(String key, RbelElement value) {
        values.add(Pair.of(key, value));
        return null;
    }

    public RbelElement put(Map.Entry<String, RbelElement> value) {
        values.add(value);
        return null;
    }

    @Override
    public RbelElement remove(Object key) {
        return removeAll(key.toString()).stream()
            .findFirst().orElse(null);
    }

    public List<RbelElement> removeAll(String key) {
        final List<RbelElement> elements = this.getAll(key);
        values.removeAll(elements);
        return elements;
    }

    @Override
    public void putAll(Map m) {
        for (Object entryRaw : m.entrySet()) {
            Map.Entry entry = (Map.Entry) entryRaw;
            values.add(Pair.of((String) entry.getKey(), (RbelElement) entry.getValue()));
        }
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Set<String> keySet() {
        return values.stream()
            .map(Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Deprecated
    public Collection<RbelElement> values() {
        throw new RbelMapUnorderedOperationException();
    }

    @Override
    @Deprecated
    public Set<Map.Entry<String, RbelElement>> entrySet() {
        throw new RbelMapUnorderedOperationException();
    }

    public Stream<Entry<String, RbelElement>> stream() {
        return values.stream();
    }

    public RbelMultiMap with(String key, RbelElement value) {
        put(key, value);
        return this;
    }

    public RbelMultiMap withSkipIfNull(String key, RbelElement value) {
        if (value != null) {
            put(key, value);
        }
        return this;
    }

    public List<RbelElement> getAll(String key) {
        return values.stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .collect(Collectors.toUnmodifiableList());
    }

    public Iterator<Entry<String, RbelElement>> iterator() {
        return values.listIterator();
    }

    private class RbelMapUnorderedOperationException extends RuntimeException {
        RbelMapUnorderedOperationException() {
            super("This collection is ordered. Therefore this operation should never be called. Use getValues() instead!");
        }
    }
}
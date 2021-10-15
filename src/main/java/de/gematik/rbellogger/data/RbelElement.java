/*
 * Copyright (c) 2021 gematik GmbH
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

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.data.facet.RbelRootFacet;
import de.gematik.rbellogger.data.facet.RbelValueFacet;
import de.gematik.rbellogger.util.RbelException;
import de.gematik.rbellogger.util.RbelPathExecutor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Builder(toBuilder = true)
@Slf4j
public class RbelElement {

    private final String uuid = UUID.randomUUID().toString();
    private final byte[] rawContent;
    private final transient RbelElement parentNode;
    private final List<RbelFacet> facets = new ArrayList<>();
    private final AtomicReference<String> note = new AtomicReference<>();

    public static RbelElement wrap(byte[] rawValue, RbelElement parentNode, Object value) {
        return new RbelElement(rawValue, parentNode)
            .addFacet(new RbelValueFacet<>(value));
    }

    public static RbelElement wrap(RbelElement parentNode, Object value) {
        return new RbelElement(value.toString().getBytes(), parentNode)
            .addFacet(new RbelValueFacet<>(value));
    }

    public Optional<String> getNote() {
        return Optional.ofNullable(note.get());
    }

    public RbelElement setNote(String value) {
        note.set(value);
        return this;
    }

    public <T> Optional<T> getFacet(Class<T> clazz) {
        return facets.stream()
            .filter(facet -> clazz.isAssignableFrom(facet.getClass()))
            .map(clazz::cast)
            .findFirst();
    }

    public <T extends RbelFacet> boolean hasFacet(Class<T> clazz) {
        return getFacet(clazz).isPresent();
    }

    public RbelElement addFacet(RbelFacet facet) {
        if (hasFacet(facet.getClass())) {
            throw new RbelException("Trying to re-add facet " + facet.getClass().getSimpleName() + "! (" + facet + ")");
        }
        facets.add(facet);
        return this;
    }

    public List<? extends RbelElement> getChildNodes() {
        return facets.stream()
            .map(RbelFacet::getChildElements)
            .flatMap(List::stream)
            .map(Entry::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<Entry<String, RbelElement>> getChildNodesWithKey() {
        return facets.stream()
            .map(RbelFacet::getChildElements)
            .flatMap(List::stream)
            .filter(el -> el.getValue() != null)
            .collect(Collectors.toList());
    }

    public void triggerPostConversionListener(final RbelConverter context) {
        for (RbelElement element : getChildNodes()) {
            element.triggerPostConversionListener(context);
        }
        context.triggerPostConversionListenerFor(this);
    }

    public List<RbelElement> traverseAndReturnNestedMembers() {
        return getChildNodes().stream()
            .map(RbelElement::traverseAndReturnNestedMembersInternal)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    // Yes, default-visibility (is called recursively)
    List<RbelElement> traverseAndReturnNestedMembersInternal() {
        log.trace("Traversing into {}: facets are {}", findNodePath(), getFacets().stream()
            .map(Object::getClass).map(Class::getSimpleName).collect(Collectors.toList()));
        if (hasFacet(RbelRootFacet.class)) {
            return List.of(this);
        } else {
            return getChildNodes().stream()
                .map(child -> child.traverseAndReturnNestedMembersInternal())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        }
    }

    public boolean isStructuralHelperElement() {
        return false;
    }

    public String findNodePath() {
        LinkedList<Optional<String>> keyList = new LinkedList<>();
        final AtomicReference<RbelElement> ptr = new AtomicReference(this);
        while (!(ptr.get().getParentNode() == null)) {
            keyList.addFirst(
                ptr.get().getParentNode().getChildNodesWithKey().stream()
                    .filter(entry -> entry.getValue() == ptr.get())
                    .map(Entry::getKey).findFirst());
            ptr.set(ptr.get().getParentNode());
        }
        return keyList.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining("."));
    }

    public Optional<RbelElement> getFirst(String key) {
        return getChildNodesWithKey().stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .findFirst();
    }

    public List<RbelElement> getAll(String key) {
        return getChildNodesWithKey().stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .collect(Collectors.toList());
    }

    public Optional<String> findKeyInParentElement() {
        return Optional.of(this)
            .map(RbelElement::getParentNode)
            .filter(Objects::nonNull)
            .stream()
            .flatMap(parent -> parent.getChildNodesWithKey().stream())
            .filter(e -> e.getValue() == this)
            .map(Entry::getKey)
            .findFirst();
    }

    public List<RbelElement> findRbelPathMembers(String rbelPath) {
        return new RbelPathExecutor(this, rbelPath)
            .execute();
    }

    public boolean isSimpleElement() {
        return false;
    }

    public String getRawStringContent() {
        if (rawContent == null) {
            return null;
        } else {
            return new String(rawContent);
        }
    }

    public <T extends RbelFacet> T getFacetOrFail(Class<T> facetClass) {
        return getFacet(facetClass).orElseThrow();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("uuid", uuid)
            .append("facets", facets)
            .append("note", note)
            .append("path", findNodePath())
            .toString();
    }

    public Optional<Object> seekValue() {
        return getFacet(RbelValueFacet.class)
            .map(RbelValueFacet::getValue)
            .filter(Objects::nonNull);
    }

    public <T> Optional<T> seekValue(Class<T> clazz) {
        return getFacet(RbelValueFacet.class)
            .map(RbelValueFacet::getValue)
            .filter(Objects::nonNull)
            .filter(clazz::isInstance)
            .map(clazz::cast);
    }

    public Optional<String> getKey() {
        if (parentNode == null) {
            return Optional.empty();
        }
        for (Entry<String, RbelElement> ptr : parentNode.getChildNodesWithKey()) {
            if (ptr.getValue() == this) {
                return Optional.ofNullable(ptr.getKey());
            }
        }
        throw new RbelException("Unable to find key for element " + this);
    }

    public void addOrReplaceFacet(RbelFacet facet) {
        if (hasFacet(facet.getClass())) {
            facets.remove(getFacet(facet.getClass()).get());
        }
        facets.add(facet);
    }

    public Optional<RbelElement> findElement(String rbelPath) {
        final List<RbelElement> resultList = findRbelPathMembers(rbelPath);
        if (resultList.isEmpty()) {
            return Optional.empty();
        }
        if (resultList.size() == 1) {
            return Optional.of(resultList.get(0));
        }
        throw new RbelPathNotUniqueException(
            "RbelPath '" + rbelPath + "' is not unique! Found " + resultList.size() + " elements, expected only one!");
    }

    private class RbelPathNotUniqueException extends RuntimeException {
        public RbelPathNotUniqueException(String s) {
            super(s);
        }
    }
}

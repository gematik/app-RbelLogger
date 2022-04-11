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

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.facet.*;
import de.gematik.rbellogger.data.util.RbelElementTreePrinter;
import de.gematik.rbellogger.util.RbelException;
import de.gematik.rbellogger.util.RbelPathExecutor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Builder(toBuilder = true)
@Slf4j
public class RbelElement {

    private final String uuid = UUID.randomUUID().toString();
    private final byte[] rawContent;
    private final transient RbelElement parentNode;
    private final List<RbelFacet> facets = new ArrayList<>();
    @Builder.Default @Setter @Getter(AccessLevel.PRIVATE)
    private Optional<Charset> charset = Optional.empty();

    public static RbelElement wrap(byte[] rawValue, @NonNull RbelElement parentNode, Object value) {
        return new RbelElement(rawValue, parentNode)
            .addFacet(new RbelValueFacet<>(value));
    }

    public static RbelElement wrap(@NonNull RbelElement parentNode, Object value) {
        return new RbelElement(value.toString().getBytes(parentNode.getElementCharset()), parentNode)
            .addFacet(new RbelValueFacet<>(value));
    }

    public <T> Optional<T> getFacet(@NonNull Class<T> clazz) {
        return facets.stream()
            .filter(facet -> clazz.isAssignableFrom(facet.getClass()))
            .map(clazz::cast)
            .findFirst();
    }

    public <T extends RbelFacet> boolean hasFacet(Class<T> clazz) {
        return getFacet(clazz).isPresent();
    }

    public RbelElement addFacet(RbelFacet facet) {
        facets.add(facet);
        return this;
    }

    public List<RbelElement> getChildNodes() {
        return facets.stream()
            .map(RbelFacet::getChildElements)
            .flatMap(List::stream)
            .map(RbelMultiMap::getRbelElement)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<RbelMultiMap> getChildNodesWithKey() {
        return facets.stream()
            .map(RbelFacet::getChildElements)
            .flatMap(List::stream)
            .filter(el -> el.getRbelElement() != null)
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
                .map(RbelElement::traverseAndReturnNestedMembersInternal)
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
                    .filter(entry -> entry.getRbelElement() == ptr.get())
                    .map(RbelMultiMap::getKey).findFirst());
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
            .map(RbelMultiMap::getRbelElement)
            .findFirst();
    }

    public List<RbelElement> getAll(String key) {
        return getChildNodesWithKey().stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(RbelMultiMap::getRbelElement)
            .collect(Collectors.toList());
    }

    public Optional<String> findKeyInParentElement() {
        return Optional.of(this)
            .map(RbelElement::getParentNode)
            .filter(Objects::nonNull)
            .stream()
            .flatMap(parent -> parent.getChildNodesWithKey().stream())
            .filter(e -> e.getRbelElement() == this)
            .map(RbelMultiMap::getKey)
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
            return new String(rawContent, getElementCharset());
        }
    }

    public Charset getElementCharset() {
        return charset
            .or(() -> Optional.ofNullable(parentNode)
                .filter(Objects::nonNull)
                .map(RbelElement::getElementCharset))
            .orElse(StandardCharsets.UTF_8);
    }

    public <T extends RbelFacet> T getFacetOrFail(Class<T> facetClass) {
        return getFacet(facetClass).orElseThrow();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("uuid", uuid)
            .append("facets", facets)
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
        for (RbelMultiMap ptr : parentNode.getChildNodesWithKey()) {
            if (ptr.getRbelElement() == this) {
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

    public String printTreeStructureWithoutColors() {
        return RbelElementTreePrinter.builder()
            .rootElement(this)
            .printColors(false)
            .build()
            .execute();
    }

    public String printTreeStructure() {
        return RbelElementTreePrinter.builder()
            .rootElement(this)
            .build()
            .execute();
    }

    public String printTreeStructure(int maximumLevels, boolean printKeys) {
        return RbelElementTreePrinter.builder()
            .rootElement(this)
            .printKeys(printKeys)
            .maximumLevels(maximumLevels)
            .build()
            .execute();
    }

    public List<RbelNoteFacet> getNotes() {
        return facets.stream()
            .flatMap(facet -> {
                if (facet instanceof RbelNestedFacet) {
                    return ((RbelNestedFacet) facet).getNestedElement().getFacets().stream();
                } else {
                    return Stream.of(facet);
                }
            })
            .filter(RbelNoteFacet.class::isInstance)
            .map(RbelNoteFacet.class::cast)
            .collect(Collectors.toUnmodifiableList());
    }

    private class RbelPathNotUniqueException extends RuntimeException {

        public RbelPathNotUniqueException(String s) {
            super(s);
        }
    }
}

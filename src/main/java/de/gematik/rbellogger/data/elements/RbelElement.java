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

package de.gematik.rbellogger.data.elements;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelMessage;
import de.gematik.rbellogger.util.RbelPathExecutor;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.platform.commons.util.ReflectionUtils;
import wiremock.com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class RbelElement {

    private final String uuid = UUID.randomUUID().toString();
    private String rawMessage;
    @JsonIgnore
    private transient RbelElement parentNode;
    @JsonIgnore
    private RbelMessage rbelMessage;
    private String note;

    public String getNote() {
        return note;
    }

    public RbelElement setNote(String note) {
        this.note = note;
        return this;
    }

    public RbelMessage getRbelMessage() {
        return rbelMessage;
    }

    public void setRbelMessage(RbelMessage message) {
        this.rbelMessage = message;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public RbelElement setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public RbelElement getParentNode() {
        return parentNode;
    }

    public void setParentNode(RbelElement parentNode) {
        this.parentNode = parentNode;
    }

    public abstract List<? extends RbelElement> getChildNodes();

    public abstract String getContent();

    /**
     * Does this element represent a logical boundary?
     * <p>
     * e.g. for a json a nested JWT is a boundary (everything beneath is only indirectly member of the JSON). Same for a
     * JSON in a HTTP-Message-Header
     *
     * @return
     */
    public boolean isNestedBoundary() {
        return true;
    }

    private List<Class> getAllRbelSuperclasses(Class startClass) {
        List<Class> classes = new ArrayList<>();
        Class currentClass = startClass;
        while (currentClass != null) {
            classes.add(currentClass);
            currentClass = currentClass.getSuperclass();
            if (!RbelElement.class.isAssignableFrom(currentClass)) {
                break;
            }
        }
        return classes;
    }

    public List<Entry<String, RbelElement>> getChildElements() {
        return getAllRbelSuperclasses(getClass()).stream()
            .flatMap(clazz -> Stream.of(clazz.getDeclaredFields()))
            .filter(field -> ReflectionUtils.isNotStatic(field))
            .filter(field -> !field.getName().equals("parentNode"))
            .filter(f -> RbelElement.class.isAssignableFrom(f.getType()))
            .map(f -> {
                try {
                    final Object o = ReflectionUtils.tryToReadFieldValue(f, this).get();
                    if (o == null) {
                        return Optional.empty();
                    }
                    return Optional.of(Pair.of(f.getName(), (RbelElement) o));
                } catch (final Exception e) {
                    return Optional.empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(e -> (Entry<String, RbelElement>) e)
            .collect(Collectors.toList());
    }

    public void triggerPostConversionListener(final RbelConverter context) {
        for (RbelElement element : getChildNodes()) {
            element.setParentNode(this);
            element.triggerPostConversionListener(context);
        }
        context.triggerPostConversionListenerFor(this);
    }

    public Map<String, RbelElement> traverseAndReturnNestedMembers() {
        return traverseAndReturnNestedMembers(getClass());
    }

    private Map<String, RbelElement> traverseAndReturnNestedMembers(
        final Class<? extends RbelElement> identityClass) {
        final Map<String, RbelElement> result = new LinkedHashMap<>();
        for (final Entry<String, RbelElement> child : getChildElements()) {
            for (final Entry<String, RbelElement> grandchild
                : child.getValue().traverseAndReturnNestedMembersWithStopAtNextBoundary(identityClass).entrySet()) {
                if (grandchild.getKey().isEmpty()) {
                    result.put(child.getKey(), grandchild.getValue());
                } else {
                    result.put(child.getKey() + "." + grandchild.getKey(), grandchild.getValue());
                }
            }
        }
        return result;
    }

    private Map<String, RbelElement> traverseAndReturnNestedMembersWithStopAtNextBoundary(
        final Class<? extends RbelElement> identityClass) {
        if (isNestedBoundary() && !getClass().isAssignableFrom(identityClass)) {
            return Map.of("", this);
        } else {
            return traverseAndReturnNestedMembers(identityClass);
        }
    }

    public String findNodePath() {
        LinkedList<Optional<String>> keyList = new LinkedList<>();
        final AtomicReference<RbelElement> ptr = new AtomicReference(this);
        while (!(ptr.get().getParentNode() == null)) {
            keyList.addFirst(ptr.get().getParentNode().getChildElements().stream()
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
        return getChildElements().stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .findFirst();
    }

    public List<RbelElement> getAll(String key) {
        return getChildElements().stream()
            .filter(entry -> entry.getKey().equals(key))
            .map(Entry::getValue)
            .collect(Collectors.toList());
    }

    public List<String> getChildKeys() {
        return getChildElements().stream()
            .map(Entry::getKey)
            .collect(Collectors.toList());
    }

    public Optional<String> findKeyInParentElement() {
        return Optional.of(this)
            .map(RbelElement::getParentNode)
            .filter(Objects::nonNull)
            .stream()
            .flatMap(parent -> parent.getChildElements().stream())
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
}

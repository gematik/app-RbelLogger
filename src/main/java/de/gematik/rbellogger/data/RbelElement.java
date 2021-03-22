/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.data;

import de.gematik.rbellogger.converter.RbelConverter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.platform.commons.util.ReflectionUtils;

public abstract class RbelElement {

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

    public Map<String, RbelElement> getChildElements() {
        final Map<String, RbelElement> collect = Stream.of(getClass().getDeclaredFields())
            .filter(f -> RbelElement.class.isAssignableFrom(f.getType()))
            .collect(Collectors.toMap(Field::getName, field -> {
                try {
                    return (RbelElement) ReflectionUtils.tryToReadFieldValue(field, this).get();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        return collect;
    }

    public void triggerPostConversionListener(final RbelConverter context) {
        context.triggerPostConversionListenerFor(this);
    }

    public Map<String, RbelElement> traverseAndReturnNestedMembers() {
        return traverseAndReturnNestedMembers(getClass());
    }

    public Map<String, RbelElement> traverseAndReturnNestedMembers(
        final Class<? extends RbelElement> identityClass) {
        final Map<String, RbelElement> result = new HashMap<>();
        for (final Entry<String, RbelElement> child : getChildElements().entrySet()) {
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

    public Map<String, RbelElement> traverseAndReturnNestedMembersWithStopAtNextBoundary(
        final Class<? extends RbelElement> identityClass) {
        if (isNestedBoundary() && !getClass().isAssignableFrom(identityClass)) {
            return Map.of("", this);
        } else {
            return traverseAndReturnNestedMembers(identityClass);
        }
    }

    private final String uuid = UUID.randomUUID().toString();

    public String getUUID() {
        return uuid;
    }
}

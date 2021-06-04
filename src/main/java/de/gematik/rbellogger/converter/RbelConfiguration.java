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

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.captures.RbelCapturer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKey;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RbelConfiguration {

    private Map<Class<? extends RbelElement>, List<BiConsumer<RbelElement, RbelConverter>>> postConversionListener
        = new HashMap<>();
    private Map<Class<? extends RbelElement>, List<BiFunction<RbelElement, RbelConverter, RbelElement>>> preConversionMappers
        = new HashMap<>();
    private List<Consumer<RbelConverter>> initializers = new ArrayList<>();
    private Map<String, RbelKey> keys = new HashMap<>();
    private RbelCapturer capturer;
    private boolean activateAsn1Parsing = true;

    public <T extends RbelElement> RbelConfiguration addPostConversionListener(Class<T> clazz,
        BiConsumer<T, RbelConverter> consumer) {
        if (!postConversionListener.containsKey(clazz)) {
            postConversionListener.put(clazz, new ArrayList<>());
        }
        postConversionListener.get(clazz).add((rawKey, context) -> consumer.accept((T) rawKey, context));
        return this;
    }

    public <T extends RbelElement> RbelConfiguration addPreConversionMapper(Class<T> clazz,
        BiFunction<T, RbelConverter, RbelElement> mapper) {
        if (!preConversionMappers.containsKey(clazz)) {
            preConversionMappers.put(clazz, new ArrayList<>());
        }
        preConversionMappers.get(clazz).add((rawKey, context) -> mapper.apply((T) rawKey, context));
        return this;
    }

    public RbelConfiguration addInitializer(Consumer<RbelConverter> initializer) {
        initializers.add(initializer);
        return this;
    }

    public RbelConfiguration addKey(final String keyId, final Key key, final int precedence) {
        keys.put(keyId, RbelKey.builder()
            .key(key)
            .keyName(keyId)
            .precedence(precedence)
            .build());
        return this;
    }

    public RbelConfiguration addCapturer(RbelCapturer capturer) {
        this.capturer = capturer;
        return this;
    }
}

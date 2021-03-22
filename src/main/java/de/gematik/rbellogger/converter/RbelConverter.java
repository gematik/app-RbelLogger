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

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.converter.listener.RbelX5cKeyReader;
import de.gematik.rbellogger.data.*;
import java.security.Key;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RbelConverter {

    private final List<RbelElement> messageHistory = new ArrayList<>();
    private final Map<String, Key> keyIdToKeyDatabase = new HashMap<>();
    private final Map<Class<? extends RbelElement>, List<BiConsumer<RbelElement, RbelConverter>>> postConversionListener
        = new HashMap<>();
    private final List<RbelConverterPlugin> converterPlugins = new ArrayList<>(List.of(
        new RbelCurlHttpMessageConverter(),
        new RbelHttpRequestConverter(),
        new RbelJwtConverter(),
        new RbelJsonConverter(),
        new RbelJweConverter(),
        new RbelPathConverter(),
        new RbelBearerTokenConverter()));

    public static RbelConverter build() {
        return build(new RbelConfiguration());
    }

    public static RbelConverter build(final RbelConfiguration configuration) {
        final RbelConverter rbelConverter = new RbelConverter();
        rbelConverter.registerListener(RbelMapElement.class, new RbelX5cKeyReader());
        if (configuration.getPostConversionListener() != null) {
            configuration.getPostConversionListener().entrySet().stream()
                .forEach(entry -> entry.getValue().stream()
                    .forEach(listener -> rbelConverter.registerListener(entry.getKey(), listener)));
            rbelConverter.postConversionListener.putAll(configuration.getPostConversionListener());
        }

        for (Consumer<RbelConverter> initializer : configuration.getInitializers()) {
            initializer.accept(rbelConverter);
        }
        rbelConverter.getKeyIdToKeyDatabase().putAll(configuration.getKeys());

        return rbelConverter;
    }

    public RbelElement convertMessage(final String input) {
        return convertMessage(new RbelStringElement(input));
    }

    public RbelElement convertMessage(final RbelElement input) {
        final RbelElement result = converterPlugins.stream()
            .filter(plugin -> plugin.canConvertElement(input, this))
            .map(plugin -> plugin.convertElement(input, this))
            .findFirst()
            .orElse(input);
        if (result instanceof RbelHttpMessage) {
            messageHistory.add(result);
        }
        result.triggerPostConversionListener(this);
        return result;
    }

    public void registerListener(final Class<? extends RbelElement> clazz,
        final BiConsumer<RbelElement, RbelConverter> listener) {
        postConversionListener.computeIfAbsent(clazz, key -> new ArrayList<>()).add(listener);
    }

    public void triggerPostConversionListenerFor(RbelElement element) {
        if (postConversionListener.containsKey(element.getClass())) {
            postConversionListener.get(element.getClass())
                .forEach(consumer -> consumer.accept(element, this));
        }
    }
}

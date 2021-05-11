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

import de.gematik.rbellogger.data.*;
import de.gematik.rbellogger.key.RbelKeyManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PUBLIC)
@Getter
@Slf4j
public class RbelConverter {

    private final List<RbelElement> messageHistory = new ArrayList<>();
    private final RbelKeyManager rbelKeyManager;
    private final RbelValueShader rbelValueShader;
    private final Map<Class<? extends RbelElement>, List<BiConsumer<RbelElement, RbelConverter>>> postConversionListener
        = new HashMap<>();
    private final Map<Class<? extends RbelElement>, List<BiFunction<RbelElement, RbelConverter, RbelElement>>> preConversionMappers
        = new HashMap<>();
    private final List<RbelConverterPlugin> converterPlugins = new ArrayList<>(List.of(
        new RbelCurlHttpMessageConverter(),
        new RbelHttpRequestConverter(),
        new RbelJwtConverter(),
        new RbelJsonConverter(),
        new RbelXmlConverter(),
        new RbelJweConverter(),
        new RbelUriConverter(),
        new RbelBearerTokenConverter(),
        new RbelBase64JsonConverter()
    ));

    public RbelElement convertMessage(final String input) {
        return convertMessage(new RbelStringElement(input));
    }

    public RbelElement convertMessage(final RbelElement rawInput) {
        log.trace("Converting {}...", rawInput);
        final RbelElement convertedInput = filterInputThroughPreConversionMappers(rawInput);
        final RbelElement result = converterPlugins.stream()
            .filter(plugin -> {
                try {
                    return plugin.canConvertElement(convertedInput, this);
                } catch (Exception e) {
                    return false;
                }
            })
            .map(plugin -> plugin.convertElement(convertedInput, this))
            .findFirst()
            .orElse(convertedInput);
        if (convertedInput.getRawMessage() == null) {
            result.setRawMessage(convertedInput.getContent());
        } else {
            result.setRawMessage(convertedInput.getRawMessage());
        }
        if (result instanceof RbelHttpMessage) {
            messageHistory.add(result);
            if (result instanceof RbelHttpResponse) {
                ((RbelHttpResponse) result).setRequest(findLastRequest());
            }
            result.triggerPostConversionListener(this);
        }
        return result;
    }

    private RbelHttpRequest findLastRequest() {
        for (int i = messageHistory.size() - 1; i >= 0; i--) {
            if (messageHistory.get(i) instanceof RbelHttpRequest) {
                return (RbelHttpRequest) messageHistory.get(i);
            }
        }
        return null;
    }

    private RbelElement filterInputThroughPreConversionMappers(final RbelElement input) {
        RbelElement value = input;
        for (BiFunction<RbelElement, RbelConverter, RbelElement> mapper : preConversionMappers.entrySet().stream()
            .filter(entry -> input.getClass().isAssignableFrom(entry.getKey()))
            .map(Entry::getValue)
            .flatMap(List::stream)
            .collect(Collectors.toList())) {
            RbelElement newValue = mapper.apply(value, this);
            if (newValue != value) {
                value = filterInputThroughPreConversionMappers(newValue);
            } else {
                value = newValue;
            }
        }
        return value;
    }

    public void registerListener(final Class<? extends RbelElement> clazz,
        final BiConsumer<RbelElement, RbelConverter> listener) {
        postConversionListener
            .computeIfAbsent(clazz, key -> new ArrayList<>())
            .add(listener);
    }

    public void triggerPostConversionListenerFor(RbelElement element) {
        final List<Class<?>> superclasses = new ArrayList<>(ClassUtils.getAllSuperclasses(element.getClass()));
        superclasses.add(element.getClass());
        superclasses
            .stream()
            .filter(postConversionListener::containsKey)
            .map(postConversionListener::get)
            .flatMap(List::stream)
            .forEach(consumer -> consumer.accept(element, this));
    }

    public void registerMapper(Class<? extends RbelElement> clazz,
        BiFunction<RbelElement, RbelConverter, RbelElement> mapper) {
        preConversionMappers
            .computeIfAbsent(clazz, key -> new ArrayList<>())
            .add(mapper);
    }
}

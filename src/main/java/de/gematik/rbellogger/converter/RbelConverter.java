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

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.facet.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.*;
import de.gematik.rbellogger.key.RbelKeyManager;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PUBLIC)
@Getter
@Slf4j
public class RbelConverter {

    @Builder.Default
    private int rbelBufferSizeInMb = 1024;
    @Builder.Default
    private boolean manageBuffer = false;
    private final List<RbelElement> messageHistory = new ArrayList<>();
    private final List<RbelBundleCriterion> bundleCriterionList = new ArrayList<>();
    private final RbelKeyManager rbelKeyManager;
    private final RbelValueShader rbelValueShader = new RbelValueShader();
    private final List<RbelConverterPlugin> postConversionListeners = new ArrayList<>();
    private final Map<Class<? extends RbelElement>, List<BiFunction<RbelElement, RbelConverter, RbelElement>>> preConversionMappers
        = new HashMap<>();
    private final List<RbelConverterPlugin> converterPlugins = new ArrayList<>(List.of(
        new RbelBase64JsonConverter(),
        new RbelHttpResponseConverter(),
        new RbelHttpRequestConverter(),
        new RbelJwtConverter(),
        new RbelHttpFormDataConverter(),
        new RbelJweConverter(),
        new RbelErpVauDecrpytionConverter(),
        new RbelUriConverter(),
        new RbelBearerTokenConverter(),
        new RbelVauEpaConverter(),
        new RbelXmlConverter(),
        new RbelJsonConverter(),
        new RbelVauKeyDeriver(),
        new RbelMtomConverter(),
        new RbelX509Converter(),
        new RbelSicctEnvelopeConverter(),
        new RbelSicctCommandConverter()
    ));
    @Builder.Default
    private long messageSequenceNumber = 0;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public RbelElement convertElement(final byte[] input, RbelElement parentNode) {
        return convertElement(RbelElement.builder()
            .parentNode(parentNode)
            .rawContent(input)
            .build());
    }

    public RbelElement convertElement(final String input, RbelElement parentNode) {
        return convertElement(RbelElement.builder()
            .parentNode(parentNode)
            .rawContent(input.getBytes(Optional.ofNullable(parentNode)
                .map(RbelElement::getElementCharset)
                    .orElse(StandardCharsets.UTF_8)))
            .build());
    }

    public RbelElement convertElement(final RbelElement rawInput) {
        log.trace("Converting {}...", rawInput);
        final RbelElement convertedInput = filterInputThroughPreConversionMappers(rawInput);
        for (RbelConverterPlugin plugin : converterPlugins) {
            try {
                plugin.consumeElement(convertedInput, this);
            } catch (RuntimeException e) {
                final String msg = "Exception during conversion with plugin '" + plugin.getClass().getName()
                    + "' (" + e.getMessage() + ")";
                log.info(msg, e);
                rawInput.addFacet(new RbelNoteFacet(msg, RbelNoteFacet.NoteStyling.ERROR));
            }
        }
        return convertedInput;
    }

    private RbelElement findLastRequest() {
        final List<RbelElement> messageHistory = getMessageHistory();
        for (int i = messageHistory.size() - 1; i >= 0; i--) {
            if (this.messageHistory.get(i)
                .getFacet(RbelHttpRequestFacet.class)
                .isPresent()) {
                return this.messageHistory.get(i);
            }
        }
        return null;
    }

    public RbelElement filterInputThroughPreConversionMappers(final RbelElement input) {
        RbelElement value = input;
        for (BiFunction<RbelElement, RbelConverter, RbelElement> mapper : preConversionMappers.entrySet().stream()
            .filter(entry -> input.getClass().isAssignableFrom(entry.getKey()))
            .map(Entry::getValue)
            .flatMap(List::stream)
            .collect(Collectors.toList())) {
            RbelElement newValue = mapper.apply(value, this);
            if (newValue != value) {
                value = filterInputThroughPreConversionMappers(newValue);
            }
        }
        return value;
    }

    public void registerListener(final RbelConverterPlugin listener) {
        postConversionListeners.add(listener);
    }

    public void triggerPostConversionListenerFor(RbelElement element) {
        for (RbelConverterPlugin postConversionListener : postConversionListeners) {
            postConversionListener.consumeElement(element, this);
        }
    }

    public void registerMapper(Class<? extends RbelElement> clazz,
                               BiFunction<RbelElement, RbelConverter, RbelElement> mapper) {
        preConversionMappers
            .computeIfAbsent(clazz, key -> new ArrayList<>())
            .add(mapper);
    }

    public void addConverter(RbelConverterPlugin converter) {
        converterPlugins.add(converter);
    }

    public RbelElement parseMessage(@NonNull byte[] content, RbelHostname sender, RbelHostname recipient) {
        final RbelElement rbelMessage = convertElement(content, null);
        return parseMessage(rbelMessage, sender, recipient);
    }

    public RbelElement parseMessage(@NonNull final RbelElement rbelElement, RbelHostname sender, RbelHostname receiver) {
        if (rbelElement.getFacet(RbelHttpResponseFacet.class).isPresent()) {
            final RbelElement lastRequest = findLastRequest();
            if (lastRequest != null) {
                rbelElement.addOrReplaceFacet(
                    rbelElement.getFacet(RbelHttpResponseFacet.class)
                        .map(RbelHttpResponseFacet::toBuilder)
                        .orElse(RbelHttpResponseFacet.builder())
                        .request(lastRequest)
                        .build());
            }

            rbelElement.addOrReplaceFacet(
                rbelElement.getFacet(RbelHttpResponseFacet.class)
                    .map(RbelHttpResponseFacet::toBuilder)
                    .orElse(RbelHttpResponseFacet.builder())
                    .request(lastRequest)
                    .build());
        }

        rbelElement.addFacet(RbelTcpIpMessageFacet.builder()
            .receiver(RbelHostnameFacet.buildRbelHostnameFacet(rbelElement, receiver))
            .sender(RbelHostnameFacet.buildRbelHostnameFacet(rbelElement, sender))
            .sequenceNumber(messageSequenceNumber++)
            .build());

        rbelElement.triggerPostConversionListener(this);
        synchronized (messageHistory) {
            messageHistory.add(rbelElement);
        }
        manageRbelBufferSize();
        return rbelElement;
    }

    public RbelConverter addPostConversionListener(RbelConverterPlugin postConversionListener) {
        postConversionListeners.add(postConversionListener);
        return this;
    }

    public void removeAllConverterPlugins() {
        converterPlugins.clear();
    }

    public void manageRbelBufferSize() {
        if (manageBuffer) {
            synchronized (messageHistory) {
                while (rbelBufferIsExceedingMaxSize()) {
                    log.info("Exceeded buffer size, dropping oldest message in history");
                    getMessageHistory().remove(0);
                }
            }
        }
    }

    private boolean rbelBufferIsExceedingMaxSize() {
        final long bufferSize = getMessageHistory().stream()
            .map(RbelElement::getRawContent)
            .mapToLong(ar -> ar.length)
            .sum();
        final boolean exceedingLimit =
            bufferSize > ((long) getRbelBufferSizeInMb() * 1024 * 1024);
        if (exceedingLimit) {
            log.info("Buffersize is {} Mb which exceeds the limit of {} Mb",
                bufferSize / (1024 ^ 2), getRbelBufferSizeInMb());
        }
        return exceedingLimit;
    }
}

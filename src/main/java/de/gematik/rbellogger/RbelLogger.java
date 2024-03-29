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

package de.gematik.rbellogger;

import de.gematik.rbellogger.captures.RbelCapturer;
import de.gematik.rbellogger.converter.RbelAsn1Converter;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.RbelBundleCriterion;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.converter.RbelX5cKeyReader;
import de.gematik.rbellogger.converter.listener.RbelBundledMessagesPlugin;
import de.gematik.rbellogger.converter.listener.RbelFileAppenderPlugin;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKeyManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import de.gematik.rbellogger.modifier.RbelModifier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class RbelLogger {

    private final RbelConverter rbelConverter;
    private final RbelCapturer rbelCapturer;
    private final RbelValueShader valueShader;
    private final RbelKeyManager rbelKeyManager;
    private final RbelModifier rbelModifier;

    public static RbelLogger build() {
        return build(new RbelConfiguration());
    }

    public static RbelLogger build(final RbelConfiguration configuration) {
        Objects.requireNonNull(configuration);

        final RbelConverter rbelConverter = RbelConverter.builder()
            .rbelKeyManager(new RbelKeyManager())
            .manageBuffer(configuration.isManageBuffer())
            .rbelBufferSizeInMb(configuration.getRbelBufferSizeInMb())
            .skipParsingWhenMessageLargerThanMb(configuration.getSkipParsingWhenMessageLargerThanMb())
            .build();

        rbelConverter.registerListener(new RbelX5cKeyReader());
        rbelConverter.getPostConversionListeners().addAll(configuration.getPostConversionListener());
        if (configuration.getPreConversionMappers() != null) {
            configuration.getPreConversionMappers().entrySet().stream()
                .forEach(entry -> entry.getValue().stream()
                    .forEach(listener -> rbelConverter.registerMapper(entry.getKey(), listener)));
            rbelConverter.getPreConversionMappers().putAll(configuration.getPreConversionMappers());
        }

        rbelConverter.registerListener(rbelConverter.getRbelValueShader().getPostConversionListener());

        for (Consumer<RbelConverter> initializer : configuration.getInitializers()) {
            initializer.accept(rbelConverter);
        }

        rbelConverter.getRbelKeyManager().addAll(configuration.getKeys());
        if (configuration.isActivateAsn1Parsing()) {
            rbelConverter.addConverter(new RbelAsn1Converter());
        }

        if (configuration.getFileSaveInfo() != null) {
            rbelConverter.addPostConversionListener(new RbelFileAppenderPlugin(configuration.getFileSaveInfo()));
        }

        rbelConverter.addPostConversionListener(new RbelBundledMessagesPlugin());

        if (configuration.getCapturer() != null) {
            configuration.getCapturer().setRbelConverter(rbelConverter);
        }

        return RbelLogger.builder()
            .rbelConverter(rbelConverter)
            .rbelCapturer(configuration.getCapturer())
            .rbelKeyManager(rbelConverter.getRbelKeyManager())
            .rbelModifier(new RbelModifier(rbelConverter.getRbelKeyManager(), rbelConverter))
            .valueShader(rbelConverter.getRbelValueShader())
            .build();
    }

    public List<RbelElement> getMessageHistory() {
        return rbelConverter.getMessageHistory();
    }

    public void addBundleCriterion(RbelBundleCriterion rbelBundleCriterion) {
        rbelConverter.getBundleCriterionList().add(rbelBundleCriterion);
    }
}

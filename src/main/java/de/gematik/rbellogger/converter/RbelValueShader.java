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

import de.gematik.rbellogger.data.elements.RbelElement;
import java.util.*;
import java.util.function.BiConsumer;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RbelValueShader {

    private final Map<String, String> jexlShadingMap = new HashMap<>();
    private final Map<String, String> jexlNoteMap = new HashMap<>();
    private final RbelJexlExecutor rbelJexlExecutor = new RbelJexlExecutor();

    public Optional<String> shadeValue(final Object element, final Optional<String> key) {
        return jexlShadingMap.entrySet().stream()
            .filter(entry -> rbelJexlExecutor.matchesAsJexlExpression(element, entry.getKey(), key))
            .map(entry -> String.format(entry.getValue(), toStringValue(element)))
            .findFirst();
    }

    public void addNote(final RbelElement element, final RbelConverter converter) {
        jexlNoteMap.entrySet().stream()
            .filter(entry -> rbelJexlExecutor.matchesAsJexlExpression(element, entry.getKey(), element.findKeyInParentElement()))
            .map(entry -> String.format(entry.getValue(), toStringValue(element)))
            .findFirst()
            .ifPresent(element::setNote);
    }

    private String toStringValue(final Object value) {
        if (value instanceof RbelElement) {
            return ((RbelElement) value).getContent();
        } else {
            return value.toString();
        }
    }

    public RbelValueShader addSimpleShadingCriterion(String attributeName, String stringFValue) {
        jexlShadingMap.put("key == '" + attributeName + "'", stringFValue);
        return this;
    }

    public RbelValueShader addJexlShadingCriterion(String jsonPathExpression, String stringFValue) {
        jexlShadingMap.put(jsonPathExpression, stringFValue);
        return this;
    }

    public RbelValueShader addJexlNoteCriterion(String jsonPathExpression, String stringFValue) {
        jexlNoteMap.put(jsonPathExpression, stringFValue);
        return this;
    }

    public BiConsumer<RbelElement, RbelConverter> getPostConversionListener() {
        return (element, converter) -> addNote(element, converter);
    }

    @Builder
    @Data
    public static class JexlMessage {

        public final String method;
        public final String url;
        public final boolean isRequest;
        public final boolean isResponse;
        public final Map<String, String> headers;
        public final String bodyAsString;
        public final RbelElement body;
    }
}

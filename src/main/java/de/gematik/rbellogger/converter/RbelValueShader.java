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

import de.gematik.rbellogger.data.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

@Slf4j
public class RbelValueShader {

    private final Map<String, String> jexlShadingMap = new HashMap<>();
    private final Map<String, String> jexlNoteMap = new HashMap<>();
    private final Map<Integer, JexlExpression> jexlExpressionCache = new HashMap<>();
    private RbelConverter rbelConverter;
    private boolean activateJexlDebugging = false;

    public Optional<String> shadeValue(final Object element, final Optional<String> key) {
        return jexlShadingMap.entrySet().stream()
            .filter(entry -> matchesAsJexlExpression(element, entry.getKey(), key))
            .map(entry -> String.format(entry.getValue(), toStringValue(element)))
            .findFirst();
    }

    public void addNote(final RbelElement element, final RbelConverter converter) {
        this.rbelConverter = converter;

        jexlNoteMap.entrySet().stream()
            .filter(entry -> matchesAsJexlExpression(element, entry.getKey(), Optional.empty()))
            .map(entry -> String.format(entry.getValue(), toStringValue(element)))
            .findFirst()
            .ifPresent(element::setNote);
    }

    private boolean matchesAsJexlExpression(Object element, String jexlExpression, Optional<String> key) {
        try {
            final JexlExpression expression = buildExpression(jexlExpression);
            final MapContext mapContext = buildJexlMapContext(element, key);

            return Optional.of(expression.evaluate(mapContext))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
        } catch (Exception e) {
            if (activateJexlDebugging) {
                log.info("Error during Jexl-Evaluation.", e);
            }
            return false;
        }
    }

    private JexlExpression buildExpression(String jexlExpression) {
        final int hashCode = jexlExpression.hashCode();
        if (jexlExpressionCache.containsKey(hashCode)) {
            return jexlExpressionCache.get(hashCode);
        }
        final JexlExpression expression = new JexlBuilder().create().createExpression(jexlExpression);
        jexlExpressionCache.put(hashCode, expression);
        return expression;
    }

    private MapContext buildJexlMapContext(Object element, Optional<String> key) {
        final MapContext mapContext = new MapContext();
        final Optional<RbelElement> parentElement = getParentElement(element);

        mapContext.set("element", element);
        mapContext.set("parent", parentElement.orElse(null));
        mapContext.set("message", findMessage(element)
            .map(this::convertToJexlMessage)
            .orElse(null));
        mapContext.set("request", tryToFindRequestMessage(element)
            .map(this::convertToJexlMessage)
            .orElse(null));
        mapContext.set("key", key
            .or(() -> tryToFindKeyFromParentMap(element, parentElement))
            .orElse(null));
        mapContext.set("path", Optional.ofNullable(element)
            .filter(RbelElement.class::isInstance)
            .map(RbelElement.class::cast)
            .map(RbelElement::findNodePath)
            .orElse(null));

        return mapContext;
    }

    private Optional<RbelHttpRequest> tryToFindRequestMessage(Object element) {
        if (rbelConverter == null || rbelConverter.getMessageHistory().size() < 2) {
            return Optional.empty();
        }
        final RbelElement rbelElement = rbelConverter.getMessageHistory()
            .get(rbelConverter.getMessageHistory().size() - 2);
        if (rbelElement instanceof RbelHttpRequest) {
            return Optional.of((RbelHttpRequest) rbelElement);
        } else {
            return Optional.empty();
        }
    }

    private JexlMessage convertToJexlMessage(RbelHttpMessage element) {
        return JexlMessage.builder()
            .isRequest(element instanceof RbelHttpRequest)
            .isResponse(element instanceof RbelHttpResponse)
            .method((element instanceof RbelHttpRequest) ? ((RbelHttpRequest) element).getMethod() : null)
            .url((element instanceof RbelHttpRequest) ? ((RbelHttpRequest) element).getPath().getOriginalUrl() : null)
            .bodyAsString(element.getBody().getContent())
            .body(element.getBody())
            .headers(element.getHeader().getElementMap()
                .entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getContent())))
            .build();
    }

    private Optional<RbelHttpMessage> findMessage(Object element) {
        if (!(element instanceof RbelElement)) {
            return Optional.empty();
        }
        RbelElement ptr = (RbelElement) element;
        while ((ptr != null) && !(ptr instanceof RbelHttpMessage)) {
            if (ptr.getParentNode() == ptr) {
                break;
            }
            ptr = ptr.getParentNode();
        }
        if (ptr instanceof RbelHttpMessage) {
            return Optional.of((RbelHttpMessage) ptr);
        } else {
            return Optional.empty();
        }
    }

    private Optional<RbelElement> getParentElement(Object element) {
        return Optional.ofNullable(element)
            .filter(RbelElement.class::isInstance)
            .map(RbelElement.class::cast)
            .map(RbelElement::getParentNode);
    }

    private Optional<String> tryToFindKeyFromParentMap(Object element, Optional<RbelElement> parent) {
        return parent
            .filter(RbelMapElement.class::isInstance)
            .map(RbelMapElement.class::cast)
            .map(RbelMapElement::getElementMap)
            .stream()
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .filter(entry -> entry.getValue() == element)
            .map(Map.Entry::getKey)
            .findFirst();
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
        return (element, converter) -> {
            addNote(element, converter);
        };
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

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
import de.gematik.rbellogger.data.elements.RbelHttpMessage;
import de.gematik.rbellogger.data.elements.RbelHttpRequest;
import de.gematik.rbellogger.data.elements.RbelHttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

@Slf4j
@Data
public class RbelJexlExecutor {

    private static final Map<Integer, JexlExpression> JEXL_EXPRESSION_CACHE = new HashMap<>();
    private static boolean ACTIVATE_JEXL_DEBUGGING = false;

    public static void activateJexlDebugging() {
        ACTIVATE_JEXL_DEBUGGING = true;
    }

    public static void deactivateJexlDebugging() {
        ACTIVATE_JEXL_DEBUGGING = false;
    }

    public boolean matchesAsJexlExpression(Object element, String jexlExpression, Optional<String> key) {
        try {
            final JexlExpression expression = buildExpression(jexlExpression);
            final MapContext mapContext = buildJexlMapContext(element, key);

            final Boolean result = Optional.of(expression.evaluate(mapContext))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);

            if (result && ACTIVATE_JEXL_DEBUGGING) {
                if (element instanceof RbelElement) {
                    log.debug("Found match: '{}' with path {} matches '{}'", element,
                        ((RbelElement) element).findNodePath(), jexlExpression);
                } else {
                    log.debug("Found match: '{}' matches '{}'", element, jexlExpression);
                }
            }

            return result;
        } catch (Exception e) {
            if (ACTIVATE_JEXL_DEBUGGING) {
                log.debug("Error during Jexl-Evaluation.", e);
            }
            return false;
        }
    }

    private JexlExpression buildExpression(String jexlExpression) {
        final int hashCode = jexlExpression.hashCode();
        if (JEXL_EXPRESSION_CACHE.containsKey(hashCode)) {
            return JEXL_EXPRESSION_CACHE.get(hashCode);
        }
        final JexlExpression expression = new JexlBuilder().create().createExpression(jexlExpression);
        JEXL_EXPRESSION_CACHE.put(hashCode, expression);
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
        mapContext.set("type", element.getClass().getSimpleName());
        if (element instanceof RbelElement) {
            mapContext.set("content", ((RbelElement) element).getContent());
        } else {
            mapContext.set("content", element.toString());
        }

        return mapContext;
    }

    private Optional<RbelHttpRequest> tryToFindRequestMessage(Object element) {
        if (!(element instanceof RbelElement)) {
            return Optional.empty();
        }
        RbelElement ptr = (RbelElement) element;
        while ((ptr != null) && !(
            (ptr instanceof RbelHttpMessage) && (ptr.getParentNode() == null))) {
            ptr = ptr.getParentNode();
        }
        if (ptr == null) {
            return Optional.empty();
        }
        if (ptr instanceof RbelHttpRequest) {
            return Optional.of((RbelHttpRequest) ptr);
        } else {
            return Optional.ofNullable(((RbelHttpResponse) ptr).getRequest());
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
            .headers(element.getHeader().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), entry -> entry.getValue().getContent())))
            .build();
    }

    private Optional<RbelHttpMessage> findMessage(Object element) {
        if (!(element instanceof RbelElement)) {
            return Optional.empty();
        }
        RbelElement ptr = (RbelElement) element;
        while ((ptr != null) && !(
            (ptr instanceof RbelHttpMessage) && (ptr.getParentNode() == null))) {
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
            .stream()
            .map(RbelElement::getChildElements)
            .flatMap(List::stream)
            .filter(entry -> entry.getValue() == element)
            .map(Map.Entry::getKey)
            .findFirst();
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

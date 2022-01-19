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
import de.gematik.rbellogger.data.facet.RbelHttpHeaderFacet;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.*;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.RbelOptions.ACTIVATE_JEXL_DEBUGGING;

@Slf4j
@Data
public class RbelJexlExecutor {

    private static final Map<Integer, JexlExpression> JEXL_EXPRESSION_CACHE = new HashMap<>();

    public boolean matchesAsJexlExpression(Object element, String jexlExpression, Optional<String> key) {
        try {
            final JexlExpression expression = buildExpression(evaluateRbelPathExpressions(jexlExpression, element));
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
                log.info("Error during Jexl-Evaluation.", e);
            }
            return false;
        }
    }

    private String evaluateRbelPathExpressions(String jexlExpression, Object element) {
        if (!(element instanceof RbelElement)
            || !jexlExpression.contains("$.")) {
            return jexlExpression;
        }
        String rbelPath = Arrays.stream(jexlExpression.split(" "))
            .filter(str -> str.startsWith("$.")
                && str.length() > 2)
            .findAny().orElseThrow();
        return jexlExpression.replace(rbelPath, "\"" + ((RbelElement) element).findElement(rbelPath)
            .map(RbelElement::getRawStringContent)
            .orElse("") + "\"");
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
        final Optional<RbelElement> message = findMessage(element);
        mapContext.set("message", message
            .map(this::convertToJexlMessage)
            .orElse(null));
        if (element instanceof RbelElement) {
            mapContext.set("charset", ((RbelElement) element).getElementCharset().displayName());
        }

        final Optional<RbelElement> requestMessage = tryToFindRequestMessage(element);
        if (requestMessage
            .filter(msg -> message.isPresent())
            .map(msg -> message.get() == msg)
            .orElse(false)) {
            mapContext.set("request", mapContext.get("message"));
            mapContext.set("isRequest", true);
            mapContext.set("isResponse", false);
        } else {
            mapContext.set("request", requestMessage
                .map(this::convertToJexlMessage)
                .orElse(null));
            mapContext.set("isRequest", false);
            mapContext.set("isResponse", true);
        }
        mapContext.set("facets", Optional.ofNullable(element)
            .filter(RbelElement.class::isInstance)
            .map(RbelElement.class::cast)
            .map(RbelElement::getFacets)
            .stream()
            .flatMap(List::stream)
            .map(Object::getClass)
            .map(Class::getSimpleName)
            .collect(Collectors.toSet()));
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
            mapContext.set("content", ((RbelElement) element).getRawStringContent());
        } else {
            mapContext.set("content", element.toString());
        }

        return mapContext;
    }

    private Optional<RbelElement> tryToFindRequestMessage(Object element) {
        if (!(element instanceof RbelElement)) {
            return Optional.empty();
        }
        final Optional<RbelElement> message = findMessage(element);
        if (message.isEmpty()) {
            return Optional.empty();
        }
        if (message.get().getFacet(RbelHttpRequestFacet.class).isPresent()) {
            return message;
        } else {
            return message
                .flatMap(el -> el.getFacet(RbelHttpResponseFacet.class))
                .map(RbelHttpResponseFacet::getRequest);
        }
    }

    private JexlMessage convertToJexlMessage(RbelElement element) {
        final Optional<RbelElement> bodyOptional = element.getFirst("body");
        return JexlMessage.builder()
            .request(element.getFacet(RbelHttpRequestFacet.class).isPresent())
            .response(element.getFacet(RbelHttpResponseFacet.class).isPresent())
            .method(element.getFacet(RbelHttpRequestFacet.class)
                .map(RbelHttpRequestFacet::getMethod).map(RbelElement::getRawStringContent)
                .orElse(null))
            .url(element.getFacet(RbelHttpRequestFacet.class)
                .map(RbelHttpRequestFacet::getPath).map(RbelElement::getRawStringContent)
                .orElse(null))
            .bodyAsString(bodyOptional.map(RbelElement::getRawStringContent).orElse(null))
            .body(bodyOptional.orElse(null))
            .headers(element.getFacet(RbelHttpMessageFacet.class)
                .map(RbelHttpMessageFacet::getHeader)
                .flatMap(el -> el.getFacet(RbelHttpHeaderFacet.class))
                .filter(RbelHttpHeaderFacet.class::isInstance)
                .map(RbelHttpHeaderFacet.class::cast)
                .map(RbelHttpHeaderFacet::entrySet)
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(e -> e.getKey(),
                    Collectors.mapping(e -> e.getValue().getRawStringContent(), Collectors.toList()))))
            .build();
    }

    private Optional<RbelElement> findMessage(Object element) {
        if (!(element instanceof RbelElement)) {
            return Optional.empty();
        }
        RbelElement ptr = (RbelElement) element;
        while (ptr.getParentNode() != null) {
            if (ptr.getParentNode() == ptr) {
                break;
            }
            ptr = ptr.getParentNode();
        }
        if (ptr.hasFacet(RbelHttpMessageFacet.class)
            && ptr.getParentNode() == null) {
            return Optional.of(ptr);
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
            .map(RbelElement::getChildNodesWithKey)
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
        public final boolean request;
        public final boolean response;
        public final Map<String, List<String>> headers;
        public final String bodyAsString;
        public final RbelElement body;
    }
}

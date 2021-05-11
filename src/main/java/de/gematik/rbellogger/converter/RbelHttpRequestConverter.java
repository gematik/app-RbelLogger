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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class RbelHttpRequestConverter extends RbelCurlHttpMessageConverter {

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        if (StringUtils.isEmpty(rbel.getContent())) {
            return false;
        }
        String firstLine = rbel.getContent().split("\n")[0].trim();
        return
            (rbel instanceof RbelStringElement)
                && (firstLine.startsWith("GET ")
                || firstLine.startsWith("POST ")
                || firstLine.startsWith("PUT ")
                || firstLine.startsWith("DELETE ")
            )
                && (firstLine.endsWith("HTTP/1.0")
                || firstLine.endsWith("HTTP/1.1")
                || firstLine.endsWith("HTTP/2.0")
            );
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        final int space = rbel.getContent().indexOf(" ");
        final int space2 = rbel.getContent().indexOf(" ", space + 1);
        final String method = rbel.getContent().substring(0, space);
        final String path = rbel.getContent().substring(space + 1, space2);

        final String[] lines = rbel.getContent().split(findLineSeparator(rbel.getContent()));
        final int bodySeparator = Arrays.asList(lines).indexOf("");

        final Map<String, List<RbelElement>> headerMap = Arrays.stream(lines)
            .limit(bodySeparator == -1 ? lines.length : bodySeparator)
            .filter(line -> !line.isEmpty() && !line.startsWith(method))
            .map(line -> parseStringToKeyValuePair(line, context))
            .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(Entry::getValue, Collectors.toList())));
        final RbelMultiValuedMapElement headers = new RbelMultiValuedMapElement(headerMap);

        final String bodyStr = bodySeparator == -1 ? "" : lines[lines.length - 1];
        final RbelElement pathElement = context.convertMessage(path);
        if (!(pathElement instanceof RbelUriElement)) {
            throw new RuntimeException("Encountered ill-formatted path: " + path);
        }

        final RbelHttpRequest rbelHttpRequest = new RbelHttpRequest(headers,
            context.convertMessage(convertBodyToRbelElement(bodyStr, headers, context)), method,
            (RbelUriElement) pathElement);
        return rbelHttpRequest;
    }

    private RbelElement convertBodyToRbelElement(final String bodyStr, final RbelMultiValuedMapElement headerMap,
        final RbelConverter context) {
        if (headerMap.containsKey("Content-Type")
            && hasContentTypeFormUrlEncoded(headerMap)) {
            return new RbelMapElement(Stream.of(bodyStr.split("&"))
                .map(param -> param.split("="))
                .filter(params -> params.length == 2)
                .map(paramList -> Pair.of(paramList[0], context.convertMessage(paramList[1])))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
        }
        return new RbelStringElement(bodyStr);
    }

    private boolean hasContentTypeFormUrlEncoded(RbelMultiValuedMapElement headerMap) {
        return headerMap.getAll("Content-Type").stream()
            .map(RbelElement::getContent)
            .anyMatch(str -> str.startsWith("application/x-www-form-urlencoded"));
    }
}

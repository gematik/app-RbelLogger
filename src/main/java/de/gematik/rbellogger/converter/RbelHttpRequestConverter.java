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
import de.gematik.rbellogger.util.RbelArrayUtils;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class RbelHttpRequestConverter extends RbelHttpResponseConverter {

    private static final String eol = "\r\n";

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        if (StringUtils.isEmpty(rbel.getContent())) {
            return false;
        }
        String firstLine = new String(getContent(rbel), StandardCharsets.US_ASCII)
            .split("\n")[0].trim();
        return (firstLine.startsWith("GET ")
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
        byte[] messageContent = getContent(rbel);
        String messageHeader = new String(messageContent, StandardCharsets.US_ASCII).split(eol + eol)[0];
        final int space = messageHeader.indexOf(" ");
        final int space2 = messageHeader.indexOf(" ", space + 1);
        final String method = messageHeader.substring(0, space);
        final String path = messageHeader.substring(space + 1, space2);

        final String[] lines = messageHeader.split(eol);
        final int bodySeparator = Arrays.asList(lines).indexOf("");

        final Map<String, List<RbelElement>> headerMap = Arrays.stream(lines)
            .limit(bodySeparator == -1 ? lines.length : bodySeparator)
            .filter(line -> !line.isEmpty() && !line.startsWith(method))
            .map(line -> parseStringToKeyValuePair(line, context))
            .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(Entry::getValue, Collectors.toList())));
        final RbelMultiValuedMapElement headers = new RbelMultiValuedMapElement(headerMap);

        final RbelElement pathElement = context.convertMessage(path);
        if (!(pathElement instanceof RbelUriElement)) {
            throw new RuntimeException("Encountered ill-formatted path: " + path);
        }

        final RbelHttpRequest rbelHttpRequest = new RbelHttpRequest(headers,
            convertBodyToRbelElement(extractBodyData(messageContent, messageHeader.length() + 4, headers),
                headers, context),
            method,
            (RbelUriElement) pathElement);
        return rbelHttpRequest;
    }

    private byte[] extractBodyData(byte[] inputData, int separator, RbelMultiValuedMapElement headerMap) {
        if (hasContentTypeMatching(headerMap, "Transfer-Encoding", "chunked")) {
            separator = new String(inputData).indexOf(eol, separator) + eol.length();
            return Arrays.copyOfRange(inputData, separator, RbelArrayUtils
                .indexOf(inputData, ("0" + eol).getBytes(), separator));
        } else {
            return Arrays.copyOfRange(inputData, separator, inputData.length);
        }
    }

    private byte[] getContent(RbelElement rbel) {
        if (rbel instanceof RbelBinaryElement) {
            return ((RbelBinaryElement) rbel).getRawData();
        } else if (rbel instanceof RbelStringElement) {
            return rbel.getContent().getBytes();
        } else {
            throw new RuntimeException("Unhandleable Content");
        }
    }

    private RbelElement convertBodyToRbelElement(final byte[] bodyData, final RbelMultiValuedMapElement headerMap,
        final RbelConverter context) {
        if (hasContentTypeMatching(headerMap, "Content-Type", "application/x-www-form-urlencoded")) {
            return new RbelMapElement(Stream.of(new String(bodyData).split("&"))
                .map(param -> param.split("="))
                .filter(params -> params.length == 2)
                .map(paramList -> Pair.of(paramList[0], context.convertMessage(paramList[1])))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
        } else if (hasContentTypeMatching(headerMap, "Content-Type", "application/octet-stream")) {
            return context.convertMessage(new RbelBinaryElement(bodyData));
        }
        return context.convertMessage(new RbelStringElement(new String(bodyData)));
    }

    private boolean hasContentTypeMatching(RbelMultiValuedMapElement headerMap, String headerKey, String prefix) {
        return headerMap.getAll(headerKey).stream()
            .map(RbelElement::getContent)
            .anyMatch(str -> str.startsWith(prefix));
    }
}

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

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpHeaderFacet;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import de.gematik.rbellogger.util.RbelArrayUtils;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.converter.RbelHttpRequestConverter.findEolInHttpMessage;

public class RbelHttpResponseConverter implements RbelConverterPlugin {

    @Override
    public void consumeElement(final RbelElement rbel, final RbelConverter converter) {
        final String content = rbel.getRawStringContent();
        if (!content.startsWith("HTTP")) {
            return;
        }

        String eol = findEolInHttpMessage(content);

        int separator = content.indexOf(eol + eol);
        if (separator == -1) {
            return;
        }
        separator += 2 * eol.length();

        final RbelElement headerElement = extractHeaderFromMessage(rbel, converter, eol);

        final byte[] bodyData = extractBodyData(rbel, separator, headerElement.getFacet(RbelHttpHeaderFacet.class).get(), eol);
        final RbelElement bodyElement = new RbelElement(bodyData, rbel);
        final RbelHttpResponseFacet rbelHttpResponse = RbelHttpResponseFacet.builder()
            .responseCode(RbelElement.builder()
                .parentNode(rbel)
                .rawContent(content.split("\\s")[1].getBytes())
                .build())
            .build();

        rbel.addFacet(rbelHttpResponse);
        rbel.addFacet(RbelHttpMessageFacet.builder()
            .header(headerElement)
            .body(bodyElement)
            .build());

        converter.convertElement(bodyElement);
    }

    public RbelElement extractHeaderFromMessage(RbelElement rbel, RbelConverter converter, String eol) {
        final String content = rbel.getRawStringContent();
        int endOfBodyPosition = content.indexOf(eol + eol);
        int endOfFirstLine = content.indexOf(eol) + eol.length();

        if (endOfBodyPosition < 0) {
            endOfBodyPosition = content.length();
        } else {
            endOfBodyPosition += 2 * eol.length();
        }

        final List<String> headerList = Arrays
            .stream(content.substring(endOfFirstLine, endOfBodyPosition).split(eol))
            .filter(line -> !line.isEmpty() && !line.startsWith("HTTP"))
            .collect(Collectors.toList());

        RbelElement headerElement = new RbelElement(
            headerList.stream().collect(Collectors.joining(eol)).getBytes(), rbel);
        final Map<String, List<RbelElement>> headerMap = headerList.stream()
            .map(line -> parseStringToKeyValuePair(line, converter, headerElement))
            .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(Entry::getValue, Collectors.toList())));
        headerElement.addFacet(new RbelHttpHeaderFacet(headerMap));

        return headerElement;
    }

    private byte[] extractBodyData(RbelElement rbel, int separator, RbelHttpHeaderFacet headerMap, String eol) {
        byte[] inputData = rbel.getRawContent();

        if (headerMap.hasValueMatching("Transfer-Encoding", "chunked")) {
            separator = rbel.getRawStringContent().indexOf(eol, separator) + eol.length();
            return Arrays.copyOfRange(inputData, Math.min(inputData.length, separator),
                RbelArrayUtils.indexOf(inputData, (eol + "0" + eol).getBytes(), separator));
        } else {
            return Arrays.copyOfRange(inputData, Math.min(inputData.length, separator), inputData.length);
        }
    }

    protected SimpleImmutableEntry<String, RbelElement> parseStringToKeyValuePair(final String line,
        final RbelConverter context, RbelElement headerElement) {
        final int colon = line.indexOf(':');
        if (colon == -1) {
            throw new IllegalArgumentException("Header malformed: '" + line + "'");
        }
        final String key = line.substring(0, colon).trim();
        final RbelElement el = new RbelElement(line.substring(colon + 1).trim().getBytes(), headerElement);

        return new SimpleImmutableEntry<>(key, context.convertElement(el));
    }
}

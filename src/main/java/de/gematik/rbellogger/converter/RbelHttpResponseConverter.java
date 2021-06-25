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

import de.gematik.rbellogger.data.elements.*;
import de.gematik.rbellogger.util.BinaryClassifier;
import de.gematik.rbellogger.util.RbelArrayUtils;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class RbelHttpResponseConverter implements RbelConverterPlugin {

    private static final String eol = "\r\n";

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        return ((rbel instanceof RbelStringElement) || (rbel instanceof RbelBinaryElement))
            && rbel.getContent().startsWith("HTTP");
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        int separator = rbel.getContent().indexOf(eol + eol);
        if (separator == -1) {
            throw new RuntimeException("Unexpected HTTP-Message encountered (did not find double \\r\\n break. "
                + "Did you read the message from the HDD and your OS messed with the line breaks?)");
        }
        separator += 2 * eol.length();

        final Map<String, List<RbelElement>> headerMap = Arrays
            .stream(rbel.getContent().substring(0, separator).split(eol))
            .filter(line -> !line.isEmpty() && !line.startsWith("HTTP"))
            .map(line -> parseStringToKeyValuePair(line, context))
            .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(Entry::getValue, Collectors.toList())));
        final RbelMultiValuedMapElement header = new RbelMultiValuedMapElement(headerMap);

        final byte[] bodyData = extractBodyData(rbel, separator, headerMap);
        boolean isBinaryData = BinaryClassifier.isBinary(bodyData);
        final RbelElement bodyElement =
            isBinaryData ? new RbelBinaryElement(bodyData) : new RbelStringElement(new String(bodyData));
        return RbelHttpResponse.builder()
            .header(header)
            .body(context.convertElement(bodyElement))
            .responseCode(Integer.parseInt(rbel.getContent().split("\\s")[1]))
            .rawMessage(rbel.getContent())
            .rawBody(bodyData)
            .build();
    }

    private byte[] extractBodyData(RbelElement rbel, int separator, Map<String, List<RbelElement>> headerMap) {
        byte[] inputData = getInputData(rbel);

        if (headerMap.containsKey("Transfer-Encoding") && headerMap.get("Transfer-Encoding") != null
            && !headerMap.get("Transfer-Encoding").isEmpty()
            && headerMap.get("Transfer-Encoding").get(0).getContent().equals("chunked")) {
            separator = rbel.getContent().indexOf(eol, separator) + eol.length();
            return Arrays.copyOfRange(inputData, Math.min(inputData.length, separator),
                RbelArrayUtils.indexOf(inputData, (eol + "0" + eol).getBytes(), separator));
        } else {
            return Arrays.copyOfRange(inputData, Math.min(inputData.length, separator), inputData.length);
        }
    }

    private byte[] getInputData(RbelElement rbel) {
        if (rbel instanceof RbelBinaryElement) {
            return ((RbelBinaryElement) rbel).getRawData();
        } else {
            return rbel.getContent().getBytes();
        }
    }

    protected SimpleImmutableEntry<String, RbelElement> parseStringToKeyValuePair(final String line,
        final RbelConverter context) {
        final int colon = line.indexOf(':');
        if (colon == -1) {
            throw new IllegalArgumentException("Header malformed: '" + line + "'");
        }
        final String key = line.substring(0, colon).trim();
        final RbelStringElement el = new RbelStringElement(line.substring(colon + 1).trim());

        return new SimpleImmutableEntry<>(key, context.convertElement(el));
    }
}

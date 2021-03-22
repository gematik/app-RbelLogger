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

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHttpResponse;
import de.gematik.rbellogger.data.RbelMapElement;
import de.gematik.rbellogger.data.RbelStringElement;
import de.gematik.rbellogger.util.RbelException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


public class RbelCurlHttpMessageConverter implements RbelConverterPlugin {

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        return (rbel instanceof RbelStringElement) && rbel.getContent().startsWith("HTTP");
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        String eol = findLineSeparator(rbel.getContent());
        int separator = rbel.getContent().indexOf(eol + eol) + 2 * eol.length();
        if (separator == -1) {
            eol = "\n";
            separator = rbel.getContent().indexOf(eol + eol) + 2 * eol.length();
        }

        final Map<String, RbelElement> headerMap = Arrays.stream(rbel.getContent().substring(0, separator).split(eol))
            .filter(line -> !line.isEmpty() && !line.startsWith("HTTP"))
            .map(line -> parseStringToKeyValuePair(line, context))
            .collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

        if (headerMap.containsKey("Transfer-Encoding") && headerMap.get("Transfer-Encoding").getContent()
            .equals("chunked")) {
            separator = rbel.getContent().indexOf(eol, separator) + eol.length();
        }
        // TODO content contains a leading eol (\r\n) - not critical for text based messages,
        //  might cause issue when looking at binary data
        final String bodyStr = rbel.getContent().substring(separator);
        return new RbelHttpResponse(
            new RbelMapElement(headerMap),
            context.convertMessage(new RbelStringElement(bodyStr)),
            Integer.parseInt(rbel.getContent().split("\\s")[1]));
    }

    public static String findLineSeparator(final String content) {
        if (content.contains("\r\n")) {
            return "\r\n";
        } else if (content.contains("\n")) {
            return "\n";
        } else {
            throw new RbelException("Could not determine linebreak for '" + content + "'");
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

        return new SimpleImmutableEntry<>(key, context.convertMessage(el));
    }
}

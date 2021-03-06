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
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelUriFacet;
import de.gematik.rbellogger.util.RbelArrayUtils;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class RbelHttpRequestConverter extends RbelHttpResponseConverter {

    @Override
    public void consumeElement(final RbelElement rbel, final RbelConverter converter) {
        final String content = rbel.getRawStringContent();
        if (StringUtils.isEmpty(content)
                || !content.contains("\n")) {
            return;
        }
        String eol = findEolInHttpMessage(content);
        if (content.split(eol).length == 0) {
            return;
        }
        String firstLine = content.split(eol)[0].trim();
        if (!((firstLine.startsWith("GET ")
            || firstLine.startsWith("POST ")
            || firstLine.startsWith("PUT ")
            || firstLine.startsWith("DELETE "))
            && (firstLine.endsWith("HTTP/1.0")
            || firstLine.endsWith("HTTP/1.1")
            || firstLine.endsWith("HTTP/2.0")
        ))) {
            return;
        }
        String messageHeader = content.split(eol + eol)[0];
        final int space = messageHeader.indexOf(" ");
        final int space2 = messageHeader.indexOf(" ", space + 1);
        final String method = messageHeader.substring(0, space);
        final String path = messageHeader.substring(space + 1, space2);

        final RbelElement headerElement = extractHeaderFromMessage(rbel, converter, eol);

        final RbelElement pathElement = converter.convertElement(path, rbel);
        if (!pathElement.hasFacet(RbelUriFacet.class)) {
            throw new RuntimeException("Encountered ill-formatted path: " + path);
        }

        final byte[] bodyData = extractBodyData(rbel.getRawContent(), messageHeader.length() + 4,
            headerElement.getFacetOrFail(RbelHttpHeaderFacet.class), eol);
        final RbelElement bodyElement = new RbelElement(bodyData, rbel);

        final RbelHttpRequestFacet httpRequest = RbelHttpRequestFacet.builder()
            .method(converter.convertElement(method, rbel))
            .path(pathElement)
            .build();
        rbel.addFacet(httpRequest);
        rbel.addFacet(RbelHttpMessageFacet.builder()
            .header(headerElement)
            .body(bodyElement)
            .build());
        converter.convertElement(bodyElement);
    }

    public static String findEolInHttpMessage(String content) {
        if (content.contains("\r\n")) {
            if (content.indexOf("\r\n") < content.indexOf("\n")) {
                return "\r\n";
            }
        }
        return "\n";
    }

    private byte[] extractBodyData(byte[] inputData, int separator, RbelHttpHeaderFacet headerMap, String eol) {
        if (headerMap.hasValueMatching("Transfer-Encoding", "chunked")) {
            separator = new String(inputData).indexOf(eol, separator) + eol.length();
            return Arrays.copyOfRange(inputData, separator, RbelArrayUtils
                .indexOf(inputData, ("0" + eol).getBytes(), separator));
        } else {
            return Arrays.copyOfRange(inputData, Math.min(inputData.length, separator), inputData.length);
        }
    }
}

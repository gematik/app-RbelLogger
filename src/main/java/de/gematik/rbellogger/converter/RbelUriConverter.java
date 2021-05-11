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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class RbelUriConverter implements RbelConverterPlugin {

    public RbelMapElement extractParameterMap(final URI uri, final RbelConverter context, String originalContent) {
        if (uri.getQuery() == null) {
            return new RbelMapElement(Map.of());
        }

        final Map<String, String> rawStringMap = Stream.of(originalContent.split("\\?")[1].split("\\&"))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toMap(param -> param.split("\\=")[0], Function.identity()));

        return new RbelMapElement(
            Stream.of(uri.getQuery().split("&"))
                .filter(param -> param.contains("="))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(
                    array -> array[0],
                    array -> context.convertMessage(array[1])
                        .setRawMessage(rawStringMap.get(array[0]))
                )));
    }

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            final URI uri = new URI(rbel.getContent());
            final boolean hasQuery = uri.getQuery() != null;
            final boolean hasProtocol = uri.getScheme() != null;
            return hasQuery || hasProtocol || rbel.getContent().startsWith("/");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        final URI uri = convertToUri(rbel);

        return new RbelUriElement(new RbelStringElement(rbel.getContent()
            .split("\\?")[0]),
            extractParameterMap(uri, context, rbel.getContent()),
            rbel.getContent());
    }

    private URI convertToUri(RbelElement target) {
        try {
            return new URI(target.getContent());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to convert Path-Element", e);
        }
    }
}

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
import de.gematik.rbellogger.data.RbelMapElement;
import de.gematik.rbellogger.data.RbelPathElement;
import de.gematik.rbellogger.data.RbelStringElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RbelPathConverter implements RbelConverterPlugin {

    public RbelMapElement extractParameterMap(final URI uri, final RbelConverter context) {
        if (uri.getQuery() == null) {
            return new RbelMapElement(Map.of());
        }
        return new RbelMapElement(
            Stream.of(uri.getQuery().split("&"))
                .filter(param -> param.contains("="))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(
                    array -> array[0],
                    array -> context.convertMessage(array[1]))));
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

        return new RbelPathElement(new RbelStringElement(rbel.getContent()
            .split("\\?")[0]),
            extractParameterMap(uri, context),
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

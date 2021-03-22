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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.gematik.rbellogger.data.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RbelJsonConverter implements RbelConverterPlugin {

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            return !JsonParser.parseString(rbel.getContent()).isJsonPrimitive();
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        return jsonElementToRbelElement(JsonParser.parseString(rbel.getContent()), context);
    }

    private RbelElement jsonElementToRbelElement(final JsonElement jsonElement, final RbelConverter context) {
        if (jsonElement.isJsonObject()) {
            final RbelMapElement rbelMapElement = new RbelMapElement(
                jsonElement.getAsJsonObject().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey,
                        v -> jsonElementToRbelElement(v.getValue(), context))));
            return new RbelJsonElement(rbelMapElement, jsonElement.toString());
        }
        if (jsonElement.isJsonArray()) {
            return new RbelJsonElement(new RbelListElement(
                StreamSupport.stream(jsonElement.getAsJsonArray()
                    .spliterator(), false)
                    .map(el -> jsonElementToRbelElement(el, context))
                    .collect(Collectors.toList())), jsonElement.toString());
        }
        if (jsonElement.isJsonPrimitive()) {
            return context.convertMessage(jsonElement.getAsString());
        }
        return new RbelNullElement();
    }
}

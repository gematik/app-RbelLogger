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
        //TODO set parent/child connections correctly!
        if (jsonElement.isJsonObject()) {
            final RbelMapElement rbelMapElement = new RbelMapElement(
                jsonElement.getAsJsonObject().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey,
                        v -> jsonElementToRbelElement(v.getValue(), context))));
            rbelMapElement.getChildNodes()
                .forEach(child -> child.setParentNode(rbelMapElement));
            rbelMapElement.getChildNodes()
                .forEach(context::triggerPostConversionListenerFor);
            return new RbelJsonElement(rbelMapElement, jsonElement.toString());
        }
        if (jsonElement.isJsonArray()) {
            final RbelJsonElement rbelJsonElement = new RbelJsonElement(new RbelListElement(
                StreamSupport.stream(jsonElement.getAsJsonArray()
                    .spliterator(), false)
                    .map(el -> jsonElementToRbelElement(el, context))
                    .collect(Collectors.toList())), jsonElement.toString());
            rbelJsonElement.getJsonElement().getChildNodes()
                .forEach(child -> child.setParentNode(rbelJsonElement.getJsonElement()));
            rbelJsonElement.getJsonElement().getChildNodes()
                .forEach(context::triggerPostConversionListenerFor);
            return rbelJsonElement;
        }
        if (jsonElement.isJsonPrimitive()) {
            final RbelElement element = context.convertMessage(jsonElement.getAsString());
            if (jsonElement.getAsJsonPrimitive().isString()) {
                element.setRawMessage("\"" + jsonElement.getAsString() + "\"");
            } else {
                element.setRawMessage(jsonElement.getAsString());
            }
            return element;
        }
        return new RbelNullElement();
    }
}

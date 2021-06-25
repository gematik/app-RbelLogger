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
import de.gematik.rbellogger.data.elements.*;
import de.gematik.rbellogger.exceptions.RbelConversionException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.StreamSupport;

public class RbelJsonConverter implements RbelConverterPlugin {

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        final String content = rbel.getContent();
        if ((content.contains("{") && content.contains("}"))
            || (content.contains("[") && content.contains("]"))) {
            try {
                JsonParser.parseString(rbel.getContent());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            return jsonElementToRbelElement(JsonParser.parseString(rbel.getContent()), context, rbel);
        } catch (Exception e) {
            throw new RbelConversionException(e);
        }
    }

    private RbelElement jsonElementToRbelElement(final JsonElement jsonElement, final RbelConverter context,
        final RbelElement parentElement) {
        if (jsonElement.isJsonObject()) {
            final LinkedHashMap<String, RbelElement> elementMap = new LinkedHashMap<>();
            final RbelMapElement rbelMapElement = new RbelMapElement(elementMap);
            final RbelJsonElement rbelJsonElement = new RbelJsonElement(rbelMapElement, jsonElement.toString());
            rbelMapElement.setParentNode(rbelJsonElement);
            rbelJsonElement.setParentNode(parentElement);
            jsonElement.getAsJsonObject().entrySet().stream()
                .forEach(entry -> elementMap.put(entry.getKey(),
                    jsonElementToRbelElement(entry.getValue(), context, rbelMapElement)));
            rbelMapElement.getChildNodes()
                .forEach(context::triggerPostConversionListenerFor);
            return rbelJsonElement;
        }
        if (jsonElement.isJsonArray()) {
            final ArrayList<RbelElement> elementList = new ArrayList<>();
            final RbelListElement rbelListElement = new RbelListElement(elementList);
            final RbelJsonElement rbelJsonElement = new RbelJsonElement(rbelListElement, jsonElement.toString());
            rbelJsonElement.setParentNode(parentElement);
            rbelListElement.setParentNode(rbelJsonElement);

            StreamSupport.stream(jsonElement.getAsJsonArray()
                .spliterator(), false)
                .map(el -> jsonElementToRbelElement(el, context, rbelListElement))
                .forEach(elementList::add);

            rbelJsonElement.getJsonElement().getChildNodes()
                .forEach(context::triggerPostConversionListenerFor);
            return rbelJsonElement;
        }
        if (jsonElement.isJsonPrimitive()) {
            final RbelStringElement stringElement = new RbelStringElement(jsonElement.getAsString());
            stringElement.setParentNode(parentElement);
            final RbelElement element = context.convertElement(stringElement);
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

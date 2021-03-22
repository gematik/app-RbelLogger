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

package de.gematik.rbellogger.data;

import de.gematik.rbellogger.converter.RbelConverter;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RbelMapElement extends RbelElement {

    private final Map<String, RbelElement> elementMap;

    @Override
    public String getContent() {
        return elementMap.entrySet().stream()
            .map(e -> "> " + e.getKey() + " = '" + e.getValue() + "'\n").collect(Collectors.joining());
    }

    @Override
    public boolean isNestedBoundary() {
        return false;
    }

    @Override
    public Map<String, RbelElement> getChildElements() {
        return elementMap;
    }

    @Override
    public void triggerPostConversionListener(RbelConverter context) {
        super.triggerPostConversionListener(context);
        for(RbelElement rbelElement : elementMap.values()) {
            rbelElement.triggerPostConversionListener(context);
        }
    }
}

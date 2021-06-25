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

package de.gematik.rbellogger.data.elements;

import java.util.*;
import java.util.Map.Entry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@Data
@RequiredArgsConstructor
public class RbelListElement extends RbelElement {

    private final List<RbelElement> elementList;

    @Override
    public List<RbelElement> getChildNodes() {
        return elementList;
    }

    @Override
    public String getContent() {
        return elementList.toString();
    }

    @Override
    public boolean isNestedBoundary() {
        return false;
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        ArrayList<Entry<String, RbelElement>> result = new ArrayList<>();
        int index = 0;
        for (RbelElement element : elementList) {
            result.add(Pair.of(String.valueOf(index++), element));
        }
        return result;
    }
}

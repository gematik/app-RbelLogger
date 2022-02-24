/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
public class RbelAsn1TaggedValueFacet implements RbelFacet {

    private final RbelElement tag;
    private final RbelElement nestedElement;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        if (nestedElement == null) {
            return List.of();
        } else {
            return List.of(
                Pair.of("content", nestedElement),
                Pair.of("tag", tag)
            );
        }
    }
}

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
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.junit.platform.commons.util.ReflectionUtils;

@Data
public class RbelJsonElement extends RbelElement {

    private final RbelElement jsonElement;
    private final String completeJsonString;

    @Override
    public Map<String, RbelElement> getChildElements() {
        if (jsonElement == null) {
            return Map.of();
        } else {
            return jsonElement.getChildElements();
        }
    }

    @Override
    public String getContent() {
        return jsonElement.getContent();
    }

    @Override
    public void triggerPostConversionListener(RbelConverter context) {
        super.triggerPostConversionListener(context);
        if (jsonElement != null) {
            jsonElement.triggerPostConversionListener(context);
        }
    }
}

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

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class RbelPathElement extends RbelElement {

    private final RbelElement basicPath;
    private final RbelMapElement queryParameter;
    private final String originalUrl;

    @Override
    public boolean isNestedBoundary() {
        return false;
    }

    @Override
    public List<RbelElement> getChildNodes() {
        return List.of(basicPath, queryParameter);
    }

    @Override
    public String getContent() {
        return originalUrl;
    }

    @Override
    public Map<String, RbelElement> getChildElements() {
        return queryParameter.getChildElements();
    }
}

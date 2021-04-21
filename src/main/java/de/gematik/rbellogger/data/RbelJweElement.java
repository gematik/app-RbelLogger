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

package de.gematik.rbellogger.data;

import de.gematik.rbellogger.converter.RbelConverter;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class RbelJweElement extends RbelElement {

    private final RbelElement header;
    private final RbelElement body;
    private final RbelJweEncryptionInfo encryptionInfo;

    @Override
    public List<RbelElement> getChildNodes() {
        return List.of(header, body, encryptionInfo);
    }

    @Override
    public String getContent() {
        return "Header Claims:\n" + header.getContent() + "\n\nBody Claims:  \n" + body.getContent();
    }
}

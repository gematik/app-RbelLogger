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

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@EqualsAndHashCode
@ToString
public class RbelHttpRequest extends RbelHttpMessage {

    private final String method;
    private final RbelUriElement path;

    @Override
    public List<RbelElement> getChildNodes() {
        final ArrayList<RbelElement> childNodes = new ArrayList<>();
        childNodes.add(path);
        childNodes.addAll(super.getChildNodes());
        return childNodes;
    }

    @Builder
    public RbelHttpRequest(RbelMultiValuedMapElement header, RbelElement body, String method, RbelUriElement path, String rawMessage, byte[] rawBody) {
        super(header, body, rawBody);
        this.method = method;
        this.path = path;
        if (rawMessage != null) {
            this.setRawMessage(rawMessage);
        }
    }

    @Override
    public String getContent() {
        return ELEMENT_SEPARATOR + "HTTP REQUEST: " + method + " - " + path + "\nHeaders:\n" +
            getHeader().getContent() + HEADER_SEPARATOR + getBody().getContent();
    }

}

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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class RbelHttpResponse extends RbelHttpMessage {

    private int responseCode;

    @Builder
    public RbelHttpResponse(RbelMapElement header, RbelElement body, int responseCode) {
        super(header, body);
        this.responseCode = responseCode;
    }

    @Override
    public String getContent() {
        return ELEMENT_SEPARATOR + "HTTP RESPONSE " + responseCode + " + \n Headers:\n" +
            getHeader().getContent() + HEADER_SEPARATOR + getBody().getContent();
    }
}

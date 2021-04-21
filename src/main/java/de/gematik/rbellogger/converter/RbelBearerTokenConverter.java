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

import static de.gematik.rbellogger.data.RbelBearerTokenElement.BEARER_TOKEN_PREFIX;

import de.gematik.rbellogger.data.RbelBearerTokenElement;
import de.gematik.rbellogger.data.RbelElement;

public class RbelBearerTokenConverter implements RbelConverterPlugin {

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        return rbel.getContent().startsWith(BEARER_TOKEN_PREFIX);
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        return new RbelBearerTokenElement(
            context.convertMessage(rbel.getContent().substring(BEARER_TOKEN_PREFIX.length())),
            rbel.getContent()
        );
    }
}

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

package de.gematik.rbellogger.data;

import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.data.facet.RbelHostnameFacet;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHttpMessageRenderer;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class RbelTcpIpMessageFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHttpMessageRenderer());
    }

    private final long sequenceNumber;
    private final RbelElement sender;
    private final RbelElement receiver;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("sender", sender),
            Pair.of("receiver", receiver)
        );
    }

    public RbelHostname getSenderHostname() {
        return sender.getFacetOrFail(RbelHostnameFacet.class).toRbelHostname();
    }

    public RbelHostname getReceiverHostname() {
        return receiver.getFacetOrFail(RbelHostnameFacet.class).toRbelHostname();
    }
}

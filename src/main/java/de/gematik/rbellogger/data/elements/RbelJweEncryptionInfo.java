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

import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.b;
import static j2html.TagCreator.p;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelFacet;
import de.gematik.rbellogger.renderer.RbelFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@RequiredArgsConstructor
@Data
@Builder(toBuilder = true)
public class RbelJweEncryptionInfo implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelJweEncryptionInfo.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                RbelHtmlRenderingToolkit renderingToolkit) {
                return childBoxNotifTitle(
                    (element.getFacetOrFail(RbelJweEncryptionInfo.class).wasDecryptable()) ? CLS_PKIOK : CLS_PKINOK)
                    .with(
                        t2("Encryption info"),
                        addNote(element),
                        p()
                            .withText("Was decrypted using Key ")
                            .with(b(element.getFacetOrFail(RbelJweEncryptionInfo.class).getDecryptedUsingKeyWithId()))
                    );
            }
        });
    }

    private final RbelElement wasDecryptable;
    private final RbelElement decryptedUsingKeyWithId;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("wasDecryptable", wasDecryptable),
            Pair.of("decryptedUsingKeyWithId", decryptedUsingKeyWithId)
        );
    }

    public boolean wasDecryptable() {
        return wasDecryptable
            .seekValue(Boolean.class)
            .orElseThrow();
    }

    public String getDecryptedUsingKeyWithId() {
        return decryptedUsingKeyWithId
            .seekValue(String.class)
            .orElse("");
    }
}

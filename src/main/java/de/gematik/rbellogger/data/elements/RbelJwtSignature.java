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

@Data
@RequiredArgsConstructor
@Builder
public class RbelJwtSignature implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelJwtSignature.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                RbelHtmlRenderingToolkit renderingToolkit) {
                return
                    childBoxNotifTitle((element.getFacetOrFail(RbelJwtSignature.class).isValid()) ? CLS_PKIOK : CLS_PKINOK).with(
                        t2("Signature"),
                        addNote(element),
                        p()
                            .withText("Was verified using Key ")
                            .with(b(element.getFacetOrFail(RbelJwtSignature.class).wasVerifiedUsing()))
                    );
            }
        });
    }

    private final RbelElement isValid;
    private final RbelElement verifiedUsing;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("isValid",isValid),
            Pair.of("verifiedUsing",verifiedUsing)
        );
    }

    public boolean isValid() {
        return isValid.seekValue(Boolean.class)
            .orElseThrow();
    }

    private String wasVerifiedUsing() {
        return Optional.ofNullable(verifiedUsing)
            .flatMap(verifiedUsing -> verifiedUsing.seekValue(String.class))
            .orElse("");
    }
}

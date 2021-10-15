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

package de.gematik.rbellogger.data.facet;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.div;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
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
@Builder
@RequiredArgsConstructor
public class RbelJwtFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHtmlFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelJwtFacet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                RbelHtmlRenderingToolkit renderingToolkit) {
                return div(
                    t1ms("JWT")
                        .with(showContentButtonAndDialog(element)),
                    addNote(element, "mb-5"),
                    ancestorTitle().with(
                        vertParentTitle().with(
                            childBoxNotifTitle(CLS_HEADER).with(
                                t2("Headers"),
                                addNote(element.getFacetOrFail(RbelJwtFacet.class).getHeader()),
                                renderingToolkit.convert(element.getFacetOrFail(RbelJwtFacet.class).getHeader())
                            ),
                            childBoxNotifTitle(CLS_BODY).with(
                                t2("Body"),
                                addNote(element.getFacetOrFail(RbelJwtFacet.class).getBody()),
                                renderingToolkit.convert(element.getFacetOrFail(RbelJwtFacet.class).getBody())
                            ),
                            renderingToolkit.convert(element.getFacetOrFail(RbelJwtFacet.class).getSignature())
                        )
                    )
                );
            }
        });
    }

    private final RbelElement header;
    private final RbelElement body;
    private final RbelElement signature;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("header", header),
            Pair.of("body", body),
            Pair.of("signature", signature)
        );
    }
}

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

package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelVauKey;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;

@RequiredArgsConstructor
@Builder
@Getter
public class RbelVauEpaFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHtmlFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelVauEpaFacet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                                                 RbelHtmlRenderingToolkit renderingToolkit) {
                return div(t1ms("VAU Encrypted Message (EPA)")
                    .with(showContentButtonAndDialog(element)))
                    .with(addNotes(element, "mb-5"))
                    .with(ancestorTitle().with(
                            vertParentTitle().with(
                                childBoxNotifTitle(CLS_BODY).with(
                                    t2("Header"),
                                    Optional.ofNullable(element.getFacetOrFail(RbelVauEpaFacet.class))
                                        .map(RbelVauEpaFacet::getAdditionalHeaders)
                                        .map(headers -> renderingToolkit.convert(headers))
                                        .orElse(span()),
                                    Optional.ofNullable(element.getFacetOrFail(RbelVauEpaFacet.class).getPVersionNumber())
                                        .map(v -> p(b("Version Number: ")).withText(v.seekValue().get().toString()))
                                        .orElse(span()),
                                    Optional.ofNullable(element.getFacetOrFail(RbelVauEpaFacet.class).getSequenceNumber())
                                        .map(v -> p(b("Sequence Number: ")).withText(v.seekValue().get().toString()))
                                        .orElse(span())
                                ),
                                childBoxNotifTitle(CLS_BODY).with(t2("Body"))
                                    .with(addNotes(element.getFacetOrFail(RbelVauEpaFacet.class).getMessage()))
                                    .with(renderingToolkit
                                        .convert(element.getFacetOrFail(RbelVauEpaFacet.class).getMessage())),
                                childBoxNotifTitle(CLS_PKIOK).with(
                                        p()
                                            .withClass(CLS_PKIOK)
                                            .withText("Was decrypted using Key ")
                                            .with(b(element.getFacetOrFail(RbelVauEpaFacet.class)
                                                    .getKeyIdUsed().getRawStringContent()),
                                                element.getFacet(RbelVauEpaFacet.class)
                                                    .flatMap(RbelVauEpaFacet::getKeyUsed)
                                                    .filter(RbelVauKey.class::isInstance)
                                                    .map(RbelVauKey.class::cast)
                                                    .map(vauKey -> vauKey.getParentKey().getKeyName())
                                                    .map(parentKeys -> span(" derived from ").with(b(parentKeys)))
                                                    .orElse(span())))
                                    .with(addNotes(element))
                            )
                        )
                    );
            }
        });
    }

    private final RbelElement message;
    private final RbelElement encryptedMessage;
    private final RbelElement sequenceNumber;
    private final RbelElement additionalHeaders;
    private final RbelElement pVersionNumber;
    private final RbelElement keyIdUsed;
    @Builder.Default
    private final Optional<RbelKey> keyUsed = Optional.empty();

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return Stream.of(
                Pair.of("message", message),
                Pair.of("encryptedMessage", encryptedMessage),
                Pair.of("sequenceNumber", sequenceNumber),
                Pair.of("additionalHeaders", additionalHeaders),
                Pair.of("pVersionNumber", pVersionNumber),
                Pair.of("keyId", keyIdUsed)
            )
            .filter(pair -> pair.getValue() != null)
            .collect(Collectors.toList());
    }
}

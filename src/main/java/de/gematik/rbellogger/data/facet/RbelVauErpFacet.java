package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;

@Builder(toBuilder = true)
@Data
public class RbelVauErpFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHtmlFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelVauErpFacet.class);
            }

            @Override
            public ContainerTag performRendering(final RbelElement element, final Optional<String> key,
                                                 final RbelHtmlRenderingToolkit renderingToolkit) {
                return div(t1ms("VAU Encrypted Message (E-Rezept)")
                    .with(showContentButtonAndDialog(element)))
                    .with(addNotes(element, "mb-5"))
                    .with(ancestorTitle().with(
                        vertParentTitle().with(
                            childBoxNotifTitle(CLS_BODY).with(
                                t2("Header"),
                                Optional.ofNullable(element.getFacetOrFail(RbelVauErpFacet.class).getPVersionNumber())
                                    .map(v -> p(b("Version Number: ")).withText(v.getRawStringContent()))
                                    .orElse(span()),
                                Optional.ofNullable(element.getFacetOrFail(RbelVauErpFacet.class).getRequestId())
                                    .map(v -> p(b("Request ID: ")).withText(v.getRawStringContent()))
                                    .orElse(span())
                            ),
                            childBoxNotifTitle(CLS_BODY).with(t2("Body"))
                                .with(addNotes(element.getFacetOrFail(RbelVauErpFacet.class).getMessage()))
                                .with(renderingToolkit.convert(element.getFacetOrFail(RbelVauErpFacet.class).getMessage(), Optional.empty())),
                            childBoxNotifTitle(CLS_PKIOK).with(
                                    p()
                                        .withClass(CLS_PKIOK)
                                        .withText("Was decrypted using Key ")
                                        .with(b(element.getFacetOrFail(RbelVauErpFacet.class).getKeyIdUsed().getRawStringContent())))
                                .with(addNotes(element))
                    )));
            }
        });
    }

    private final RbelElement message;
    private final RbelElement encryptedMessage;
    private final RbelElement requestId;
    private final RbelElement pVersionNumber;
    private final RbelElement keyIdUsed;
    private final RbelElement responseKey;
    private final RbelElement decryptedPString;
    @Builder.Default
    private final Optional<RbelKey> keyUsed = Optional.empty();

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return Stream.of(
                Pair.of("message", message),
                Pair.of("encryptedMessage", encryptedMessage),
                Pair.of("requestId", requestId),
                Pair.of("pVersionNumber", pVersionNumber),
                Pair.of("keyId", keyIdUsed),
                Pair.of("responseKey", responseKey),
                Pair.of("decryptedPString", decryptedPString)
            )
            .filter(pair -> pair.getValue() != null)
            .collect(Collectors.toList());
    }
}

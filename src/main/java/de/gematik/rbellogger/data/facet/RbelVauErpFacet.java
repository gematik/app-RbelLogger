package de.gematik.rbellogger.data.facet;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.b;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Builder(toBuilder = true)
@Data
public class RbelVauErpFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelVauErpFacet.class);
            }

            @Override
            public ContainerTag performRendering(final RbelElement element, final  Optional<String> key,
                final RbelHtmlRenderingToolkit renderingToolkit) {
                return div(
                    t1ms("VAU Encrypted Message (E-Rezept)")
                        .with(showContentButtonAndDialog(element)),
                    addNote(element, "mb-5"),
                    ancestorTitle().with(
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
                            childBoxNotifTitle(CLS_BODY).with(
                                t2("Body"),
                                addNote(element.getFacetOrFail(RbelVauErpFacet.class).getMessage()),
                                renderingToolkit.convert(element.getFacetOrFail(RbelVauErpFacet.class).getMessage(), Optional.empty())
                            ),
                            childBoxNotifTitle(CLS_PKIOK).with(
                                p()
                                    .withClass(CLS_PKIOK)
                                    .withText("Was decrypted using Key ")
                                    .with(b(element.getFacetOrFail(RbelVauErpFacet.class).getKeyIdUsed().getRawStringContent())),
                                addNote(element)
                            ))
                    )
                );
            }
        });
    }

    private final RbelElement message;
    private final RbelElement encryptedMessage;
    private final RbelElement requestId;
    private final RbelElement pVersionNumber;
    private final RbelElement keyIdUsed;
    private final RbelElement responseKey;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return Stream.of(
            Pair.of("message", message),
            Pair.of("encryptedMessage", encryptedMessage),
            Pair.of("requestId", requestId),
            Pair.of("pVersionNumber", pVersionNumber),
            Pair.of("keyId", keyIdUsed),
            Pair.of("responseKey", responseKey)
        )
            .filter(pair -> pair.getValue() != null)
            .collect(Collectors.toList());
    }
}

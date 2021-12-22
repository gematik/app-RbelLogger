package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.util.MtomPart;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import j2html.tags.ContainerTag;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;
import static j2html.TagCreator.b;

@Data
public class RbelMtomFacet implements RbelFacet {

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelHtmlFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelMtomFacet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                                                 RbelHtmlRenderingToolkit renderingToolkit) {
                return div(
                    t1ms("XML XOP/MTOM Message")
                        .with(showContentButtonAndDialog(element)))
                    .with(addNotes(element, "mb-5"))
                    .with(ancestorTitle().with(
                        vertParentTitle().with(
                            childBoxNotifTitle(CLS_BODY).with(
                                t2("Content Type"),
                                Optional.ofNullable(element.getFacetOrFail(RbelMtomFacet.class))
                                    .map(RbelMtomFacet::getContentType)
                                    .map(headers -> renderingToolkit.convert(headers))
                                    .orElse(span())
                            ),
                            childBoxNotifTitle(CLS_BODY)
                                .with(t2("Reconstructed Message"))
                                .with(addNotes(element.getFacetOrFail(RbelMtomFacet.class).getReconstructedMessage()))
                                .with(renderingToolkit
                                    .convert(element.getFacetOrFail(RbelMtomFacet.class).getReconstructedMessage()))
                        )
                    )
                );
            }
        });
    }


    private final RbelElement contentType;
    private final RbelElement reconstructedMessage;
    private final List<MtomPart> mtomParts;

    @Override
    public List<Map.Entry<String, RbelElement>> getChildElements() {
        return List.of(
            Pair.of("contentType", contentType),
            Pair.of("reconstructedMessage", reconstructedMessage)
        );
    }
}

package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import de.gematik.rbellogger.util.GenericPrettyPrinter;
import j2html.tags.ContainerTag;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.ancestorTitle;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.vertParentTitle;
import static j2html.TagCreator.*;

@Data
@Builder(toBuilder = true)
public class RbelAsn1Facet implements RbelFacet {

    private static final GenericPrettyPrinter<ASN1Encodable> ASN1_PRETTY_PRINTER = new GenericPrettyPrinter<>(
            asn1 -> (asn1 instanceof ASN1Sequence) || (asn1 instanceof ASN1Set),
            Object::toString,
            asn1 -> StreamSupport.stream(((Iterable<ASN1Encodable>) asn1).spliterator(), false)
    );

    static {
        RbelHtmlRenderer.registerFacetRenderer(new RbelFacetRenderer() {
            @Override
            public boolean checkForRendering(RbelElement element) {
                return element.hasFacet(RbelAsn1Facet.class);
            }

            @Override
            public ContainerTag performRendering(RbelElement element, Optional<String> key,
                                                 RbelHtmlRenderingToolkit renderingToolkit) {
                return div(
                        pre(ASN1_PRETTY_PRINTER.prettyPrint(
                                element.getFacetOrFail(RbelAsn1Facet.class).getAsn1Content()))
                                .withClass("binary"),
                        br(),
                        ancestorTitle().with(
                                vertParentTitle().with(renderingToolkit.convertNested(element)))
                );
            }
        });
    }

    private final RbelElement unparsedBytes;
    private final ASN1Encodable asn1Content;

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of(
                Pair.of("unparsedBytes", unparsedBytes)
        );
    }
}

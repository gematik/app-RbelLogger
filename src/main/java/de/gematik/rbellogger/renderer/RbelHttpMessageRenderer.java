package de.gematik.rbellogger.renderer;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.collapsibleCard;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.showContentButtonAndDialog;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import de.gematik.rbellogger.exceptions.RbelRenderingException;
import j2html.tags.ContainerTag;
import java.util.Optional;

public class RbelHttpMessageRenderer implements RbelFacetRenderer {

    public static ContainerTag buildAddressInfo(final RbelElement element) {
        if (!element.hasFacet(RbelTcpIpMessageFacet.class)) {
            return span();
        }
        final RbelTcpIpMessageFacet messageFacet = element.getFacet(RbelTcpIpMessageFacet.class).get();
        if (!messageFacet.getSender().seekValue().isPresent() &&
            !messageFacet.getReceiver().seekValue().isPresent()) {
            return span();
        }
        final RbelHostname left;
        final RbelHostname right;
        final String icon;
        if (element.hasFacet(RbelHttpRequestFacet.class)) {
            left = messageFacet.getSender().seekValue(RbelHostname.class).orElse(null);
            right = messageFacet.getReceiver().seekValue(RbelHostname.class).orElse(null);
            icon = "fa-arrow-right";
        } else {
            left = messageFacet.getReceiver().seekValue(RbelHostname.class).orElse(null);
            right = messageFacet.getSender().seekValue(RbelHostname.class).orElse(null);
            icon = "fa-arrow-left";
        }
        return span()
            .withText(left == null ? "" : left.toString())
            .with(icon(icon))
            .with(text(right == null ? "" : right.toString()))
            .withClass("is-size-6 ml-4");
    }

    @Override
    public boolean checkForRendering(RbelElement element) {
        return element.hasFacet(RbelHttpMessageFacet.class);
    }

    @Override
    public ContainerTag performRendering(final RbelElement element, final Optional<String> key,
        final RbelHtmlRenderingToolkit renderingToolkit) {
        final RbelHttpMessageFacet messageFacet = element.getFacet(RbelHttpMessageFacet.class)
            .orElseThrow(() -> new RbelRenderingException("Unqualified call!"));
        final Optional<RbelHttpRequestFacet> requestFacet = element.getFacet(RbelHttpRequestFacet.class);
        final Optional<RbelHttpResponseFacet> responseFacet = element.getFacet(RbelHttpResponseFacet.class);
        return collapsibleCard(
            //////////////////////////////// TITLE (+path, response-code...) //////////////////////////////////
            div().with(
                a().withName(element.getUuid()),
                i().withClasses("fas fa-toggle-on toggle-icon is-pulled-right mr-3 is-size-3",
                    requestFacet.map(f -> "has-text-link").orElse("has-text-success")),
                showContentButtonAndDialog(element),
                h1(renderingToolkit.constructMessageId(element),
                    i().withClass(requestFacet.map(f -> "fas fa-share").orElse("fas fa-reply")),
                    requestFacet.map(f -> text(" " + f.getMethod().getRawStringContent() + " ")).orElse(text("")),
                    requestFacet.map(f -> text("Request")).orElse(text("Response")))
                    .with(buildAddressInfo(element))
                    .withClass(requestFacet.map(f -> "title has-text-link").orElse("title has-text-success")),
                addNote(element),
                div().withClass("container is-widescreen").with(
                    requestFacet.map(f ->
                        div(renderingToolkit.convert(requestFacet.get().getPath(), Optional.empty()))
                            .withClass("is-family-monospace title is-size-4")
                            .with(addNote(requestFacet.get().getPath())))
                        .orElseGet(() -> t1ms(responseFacet.get().getResponseCode().getRawStringContent() + ""))
                )
            ).withClass("full-width"),
            //////////////////////////////// HEADER & BODY //////////////////////////////////////
            ancestorTitle().with(
                div().withClass("tile is-parent is-vertical pr-3").with(
                    childBoxNotifTitle(CLS_HEADER).with(
                        requestFacet.map(f -> t2("REQ Headers")).orElseGet(() -> t2("RES Headers")),
                        addNote(messageFacet.getHeader()),
                        renderingToolkit.convert(messageFacet.getHeader(), Optional.empty())
                    ),
                    childBoxNotifTitle(CLS_BODY).with(
                        requestFacet.map(f -> t2("REQ Body")).orElseGet(() -> t2("RES Body")),
                        addNote(messageFacet.getBody()),
                        renderingToolkit.convert(messageFacet.getBody(), Optional.empty())
                    )
                )
            ));
    }
}

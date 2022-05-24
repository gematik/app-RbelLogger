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

package de.gematik.rbellogger.renderer;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderer.*;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.*;
import static j2html.TagCreator.*;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.*;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RbelMessageRenderer implements RbelHtmlFacetRenderer {

    public static ContainerTag buildAddressInfo(final RbelElement element) {
        if (!element.hasFacet(RbelTcpIpMessageFacet.class)) {
            return span();
        }
        final RbelTcpIpMessageFacet messageFacet = element.getFacetOrFail(RbelTcpIpMessageFacet.class);
        if (messageFacet.getSender().getFacet(RbelHostnameFacet.class).isEmpty() &&
            messageFacet.getReceiver().getFacet(RbelHostnameFacet.class).isEmpty()) {
            return span();
        }
        final String left;
        final String right;
        final String icon;
        final Optional<Boolean> isRequest = determineIsRequest(element);
        if (isRequest.isEmpty() || isRequest.get()) {
            left = messageFacet.getSender().getFacet(RbelHostnameFacet.class).map(RbelHostnameFacet::toString)
                .orElse(null);
            right = messageFacet.getReceiver().getFacet(RbelHostnameFacet.class).map(RbelHostnameFacet::toString)
                .orElse(null);
            icon = "fa-arrow-right";
        } else {
            left = messageFacet.getReceiver().getFacet(RbelHostnameFacet.class).map(RbelHostnameFacet::toString)
                .orElse(null);
            right = messageFacet.getSender().getFacet(RbelHostnameFacet.class).map(RbelHostnameFacet::toString)
                .orElse(null);
            icon = "fa-arrow-left";
        }

        return span()
            .withText(left == null ? "" : left)
            .with(icon(icon))
            .with(text(right == null ? "" : right))
            .withClass("is-size-6 ml-4");
    }

    private static Optional<Boolean> determineIsRequest(RbelElement element) {
        if (element.hasFacet(RbelRequestFacet.class)) {
            return Optional.of(true);
        } else if (element.hasFacet(RbelResponseFacet.class)) {
            return Optional.of(false);
        } else {
            return Optional.empty();
        }
    }

    public static ContainerTag buildTimingInfo(final RbelElement element) {
        if (!element.hasFacet(RbelMessageTimingFacet.class)) {
            return span();
        }
        final RbelMessageTimingFacet timingFacet = element.getFacetOrFail(RbelMessageTimingFacet.class);
        return span()
            .with(icon("fa-clock"))
            .withText(timingFacet.getTransmissionTime().format(DateTimeFormatter.ISO_TIME))
            .withClass("is-size-6 ml-4 ");
    }

    @Override
    public boolean checkForRendering(RbelElement element) {
        return element.hasFacet(RbelHttpMessageFacet.class)
            && element.getParentNode() != null; // prevent recursive call for non-http messages
    }

    @Override
    public ContainerTag performRendering(final RbelElement element, final Optional<String> key,
                                         final RbelHtmlRenderingToolkit renderingToolkit) {
        final Optional<RbelHttpMessageFacet> httpMessageFacet = element.getFacet(RbelHttpMessageFacet.class);
        final Optional<RbelHttpRequestFacet> httpRequestFacet = element.getFacet(RbelHttpRequestFacet.class);
        final Optional<RbelHttpResponseFacet> httpResponseFacet = element.getFacet(RbelHttpResponseFacet.class);
        final Optional<Boolean> isRequest = determineIsRequest(element);
        //////////////////////////////// TITLE (+path, response-code...) //////////////////////////////////
        List<DomContent> messageTitleElements = new ArrayList<>();
        messageTitleElements.add(a().withName(element.getUuid()));
        messageTitleElements.add(i().withClasses("fas fa-toggle-on toggle-icon is-pulled-right mr-3 is-size-3",
            httpRequestFacet.map(f -> "has-text-link").orElse("has-text-success")));
        messageTitleElements.add(showContentButtonAndDialog(element));
        messageTitleElements.add(h1(renderingToolkit.constructMessageId(element),
            getRequestOrReplySymbol(isRequest),
            httpRequestFacet.map(f -> text(" " + f.getMethod().getRawStringContent() + " ")).orElse(text("")),
            isRequest.map(req -> req ? text("Request") : text("Response")).orElse(text("")))
            .with(buildAddressInfo(element))
            .with(buildTimingInfo(element))
            .withClass(isRequest
                .map(req -> req ? "title has-text-link" : "title has-text-success")
                .orElse("title")));
        if (httpMessageFacet.isPresent()) {
            messageTitleElements.add(div().withClass("container is-widescreen").with(
                httpRequestFacet.map(f ->
                        div(renderingToolkit.convert(httpRequestFacet.get().getPath(), Optional.empty()))
                            .withClass("is-family-monospace title is-size-4")
                            .with(addNotes(httpRequestFacet.get().getPath())))
                    .orElseGet(() -> t1ms(httpResponseFacet.get().getResponseCode().getRawStringContent() + ""))
            ));
        }
        messageTitleElements.addAll(addNotes(element));
        //////////////////////////////// HEADER & BODY //////////////////////////////////////
        List<DomContent> messageBodyElements = new ArrayList<>();
        if (httpMessageFacet.isPresent()) {
            messageBodyElements.add(ancestorTitle().with(
                div().withClass("tile is-parent is-vertical pr-3").with(
                    childBoxNotifTitle(CLS_HEADER).with(
                        httpRequestFacet.map(f -> t2("REQ Headers")).orElseGet(() -> t2("RES Headers")),
                        renderingToolkit.convert(httpMessageFacet.get().getHeader(), Optional.empty())
                    ),
                    childBoxNotifTitle(CLS_BODY).with(
                        httpRequestFacet.map(f -> t2("REQ Body")).orElseGet(() -> t2("RES Body")),
                        renderingToolkit.convert(httpMessageFacet.get().getBody(), Optional.empty())
                    )
                )
            ));
        } else {
            // non parseable message
            messageBodyElements.add(renderingToolkit.convert(element));
        }
        return collapsibleCard(
            div()
                .with(messageTitleElements)
                .withClass("full-width"),
            ancestorTitle().with(messageBodyElements));
    }

    private DomContent getRequestOrReplySymbol(Optional<Boolean> isRequestOptional) {
        return isRequestOptional
            .map(isRequest -> {
                if (isRequest) {
                    return i().withClass("fas fa-share");
                } else {
                    return i().withClass("fas fa-reply");
                }
            })
            .orElse(span());
    }
}

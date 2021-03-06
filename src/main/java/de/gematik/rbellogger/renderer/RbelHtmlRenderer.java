package de.gematik.rbellogger.renderer;

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

import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelAsn1Facet;
import de.gematik.rbellogger.util.BinaryClassifier;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static j2html.TagCreator.*;

@Slf4j
@Getter
public class RbelHtmlRenderer {

    private static final List<RbelFacetRenderer> htmlRenderer = new ArrayList<>();
    private final RbelValueShader rbelValueShader;
    @Setter
    private boolean renderAsn1Objects = false;
    @Setter
    private boolean renderNestedObjectsWithoutFacetRenderer = false;
    @Setter
    private String title = "RBelLogger";
    @Setter
    private String subTitle = "<p>The [R]everse [B]ridle [E]vent [L]ogger pays tribute to the fact "
        + "that many agile projects' specifications, alas somewhat complete, "
        + "lack specificality. Using PoCs most of the time does not resolve this as the code is not "
        + "well enough documented and communication between nodes is not observable or "
        + "logged in a well enough readable manner.</p> "
        + "<p>This is where the RBeL Logger comes into play.</p> "
        + "<p>Attaching it to a network, RestAssured or Wiremock interface or instructing it to read from a recorded PCAP file, "
        + "produces this shiny communication log supporting Plain HTTP, JSON, JWT and even JWE!</p>";

    public RbelHtmlRenderer(final RbelValueShader rbelValueShader) {
        this.rbelValueShader = rbelValueShader;
    }

    public RbelHtmlRenderer() {
        rbelValueShader = new RbelValueShader();
    }

    public static String render(final List<RbelElement> elements) {
        return render(elements, new RbelValueShader());
    }

    public static String render(final List<RbelElement> elements, final RbelValueShader valueShader) {
        return new RbelHtmlRenderer(valueShader)
            .performRendering(elements);
    }

    public static ContainerTag collapsibleCard(final ContainerTag title, final ContainerTag body) {
        return
            div().withClass("container page-break mx-3 my-6").with(
                div().withClass("card full-width")
                    .with(
                        header().withClass("card-header")
                            .with(
                                div().withClass("card-header-title card-toggle")
                                    .with(title)
                            ),
                        div().withClass("card-content")
                            .with(
                                div().with(body)
                            )
                    )
            );
    }

    public static DomContent showContentButtonAndDialog(final RbelElement el) {
        final String id = "dialog" + RandomStringUtils.randomAlphanumeric(20);//NOSONAR
        return span().with(
            a().withClass("button modal-button is-pulled-right mx-3")
                .attr("data-target", id)
                .with(span().withClass("icon is-small").with(
                    i().withClass("fas fa-align-left")
                )),
            div().withClass("modal")
                .withId(id)
                .with(
                    div().withClass("modal-background"),
                    div().withClass("modal-content").with(
                        article().withClass("message").with(
                            div().withClass("message-header").with(
                                p("Raw content of " + el.findNodePath()),
                                button().withClass("delete").attr("aria-label", "delete")
                            ),
                            div().withClass("message-body")
                                .with(pre(
                                    BinaryClassifier.isBinary(el.getRawContent()) ?
                                        Hex.toHexString(el.getRawContent()) :
                                        el.getRawStringContent())
                                    .withStyle("white-space: pre-wrap;word-wrap: break-word;"))
                        )
                    ),
                    button().withClass("modal-close is-large")
                        .attr("aria-label", "close")
                )
        );
    }

    public static void registerFacetRenderer(RbelFacetRenderer rbelFacetRenderer) {
        htmlRenderer.add(rbelFacetRenderer);
    }

    public String doRender(final List<RbelElement> elements) {
        return performRendering(elements);
    }

    @SneakyThrows
    private String performRendering(final List<RbelElement> elements) {
        RbelHtmlRenderingToolkit renderingToolkit = new RbelHtmlRenderingToolkit(this);

        final List effectiveElementList = new ArrayList(elements);
        renderingToolkit.initializeElementIndexMap(effectiveElementList);

        return renderingToolkit.renderDocument(effectiveElementList);
    }

    public Optional<ContainerTag> convert(final RbelElement element, final Optional<String> key,
                                          final RbelHtmlRenderingToolkit renderingToolkit) {
        if (element.getFacets().isEmpty() && ArrayUtils.isEmpty(element.getRawContent())) {
            return Optional.empty();
        }
        return htmlRenderer.stream()
            .filter(renderer -> renderAsn1Objects || !(renderer.getClass().getName().startsWith(RbelAsn1Facet.class.getName())))
            .filter(renderer -> renderer.checkForRendering(element))
            .map(renderer -> renderer.performRendering(element, key, renderingToolkit))
            .findAny();
    }

    public String getEmptyPage() {
        return performRendering(List.of());
    }
}

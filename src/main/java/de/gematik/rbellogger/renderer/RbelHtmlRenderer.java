/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.renderer;

import static j2html.TagCreator.*;
import com.google.gson.*;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.data.*;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.EmptyTag;
import j2html.tags.UnescapedText;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RbelHtmlRenderer {

    public RbelHtmlRenderer() {
        this(new RbelValueShader());
    }

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final String CLS_HEADER = "is-primary";
    private static final String CLS_BODY = "is-info";
    private static final String CLS_PKIOK = "is-success";
    private static final String CLS_PKINOK = "is-primary";
    private static final Map<String, String> elementIndices = new HashMap<>();
    private final Map<Class<? extends RbelElement>, BiFunction<RbelElement, Optional<String>, ContainerTag>> htmlRenderer = new HashMap<>();
    private final RbelValueShader rbelValueShader;
    private String currentKey;

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

    {
        htmlRenderer.put(RbelHttpRequest.class, (el, key) -> collapsibleCard(
            div().with(
                a().withName(el.getUUID()),
                i().withClass("fas fa-toggle-on toggle-icon is-pulled-right mr-3 is-size-3 has-text-link"),
                h1(span(elementIndices.get(el.getUUID())).withClass("tag is-info is-light mr-3 is-size-3"),
                    i().withClass("fas fa-share"), text(" " + ((RbelHttpRequest) el).getMethod() + " Request"))
                    .withClass("title has-text-link"),
                div().withClass("container is-widescreen").with(
                    div(convert(((RbelHttpRequest) el).getPath(), null))
                        .withClass("is-family-monospace title is-size-4"))
            ).withClass("full-width"),
            ancestorTitle().with(
                div().withClass("tile is-parent is-vertical ").with(
                    childBoxNotifTitle(CLS_HEADER).with(
                        t2("REQ Headers"),
                        convert(((RbelHttpRequest) el).getHeader(), Optional.empty())
                    ),
                    childBoxNotifTitle(CLS_BODY).with(
                        t2("REQ Body"),
                        convert(((RbelHttpRequest) el).getBody(), Optional.empty())
                    )
                )
            )));

        htmlRenderer.put(RbelHttpResponse.class, (el, key) -> collapsibleCard(
            div().with(
                a().withName(el.getUUID()),
                i().withClass("fas fa-toggle-on toggle-icon is-pulled-right mr-3 is-size-3 has-text-success"),
                h1(span(elementIndices.get(el.getUUID())).withClass("tag is-info is-light mr-3 is-size-3"),
                    i().withClass("fas fa-reply"), text(" Response")).withClass("title has-text-success")
            ).withClass("full-width"),
            div().withClass("container is-widescreen").with(
                t1ms(((RbelHttpResponse) el).getResponseCode() + ""),
                ancestorTitle().with(
                    div().withClass("tile is-parent is-vertical ").with(
                        childBoxNotifTitle(CLS_HEADER).with(
                            t2("RES Headers"),
                            convert(((RbelHttpResponse) el).getHeader(), Optional.empty())
                        ),
                        div().withClass("tile is-child is-vertical box notification is-info").with(
                            t2("RES Body"),
                            convert(((RbelHttpResponse) el).getBody(), Optional.empty())
                        )
                    )
                )
            )));

        htmlRenderer.put(RbelMapElement.class, (el, key) -> table().withClass("table").with(
            thead(
                tr(th("name"), th("value"))
            ),
            tbody().with(
                ((RbelMapElement) el).getElementMap().entrySet().stream()
                    .map(entry -> tr(
                        td(pre(entry.getKey())),
                        td(pre().with(convert(entry.getValue(), Optional.ofNullable(entry.getKey())))
                            .withClass("value"))
                    ))
                    .collect(Collectors.toList())
            )
        ));
        htmlRenderer.put(RbelJsonElement.class, (el, key) ->
            ancestorTitle().with(
                vertParentTitle().with(
                    div().withClass("tile is-child ").with(
                        pre(
                            GSON.toJson(
                                shadeJson(
                                    JsonParser.parseString(((RbelJsonElement) el).getCompleteJsonString()),
                                    Optional.empty()
                                )))
                            .withClass("json")
                    ).with(convertNested(el)))));
        htmlRenderer.put(RbelJwtElement.class, (el, key) -> div(
            t1ms("JWT"),
            ancestorTitle().with(
                vertParentTitle().with(
                    childBoxNotifTitle(CLS_HEADER).with(
                        t2("Headers"),
                        convert(((RbelJwtElement) el).getHeader(), Optional.empty())
                    ),
                    childBoxNotifTitle(CLS_BODY).with(
                        t2("Body"),
                        convert(((RbelJwtElement) el).getBody(), Optional.empty())
                    ),
                    convert(((RbelJwtElement) el).getSignature(), Optional.empty())
                )
            )
        ));
        htmlRenderer.put(RbelJweElement.class, (el, key) -> div(
            t1ms("JWE"),
            ancestorTitle().with(
                vertParentTitle().with(
                    childBoxNotifTitle(CLS_HEADER).with(
                        t2("Headers"),
                        convert(((RbelJweElement) el).getHeader(), Optional.empty())
                    ),
                    childBoxNotifTitle(CLS_BODY).with(
                        t2("Body"),
                        convert(((RbelJweElement) el).getBody(), Optional.empty())
                    ),
                    convert(((RbelJweElement) el).getEncryptionInfo(), Optional.empty())
                )
            )
        ));
        htmlRenderer.put(RbelJwtSignature.class, (el, key) ->
            childBoxNotifTitle((((RbelJwtSignature) el).isValid()) ? CLS_PKIOK : CLS_PKINOK).with(
                t2("Signature"),
                p()
                    .withText("Was verified using Key ")
                    .with(b(((RbelJwtSignature) el).getVerifiedUsing()))
            ));
        htmlRenderer.put(RbelJweEncryptionInfo.class, (el, key) ->
            childBoxNotifTitle((((RbelJweEncryptionInfo) el).isWasDecryptable()) ? CLS_PKIOK : CLS_PKINOK).with(
                t2("Encryption info"),
                p()
                    .withText("Was decrypted using Key ")
                    .with(b(((RbelJweEncryptionInfo) el).getDecryptedUsingKeyWithId()))
            ));
        htmlRenderer.put(RbelNullElement.class, (el, key) -> div("- empty -"));

        htmlRenderer.put(RbelPathElement.class, (el, key) -> {
            final ContainerTag urlContent = renderUrlContent(el);
            if (el.getChildElements().isEmpty()) {
                return div().with(urlContent);
            } else {
                return ancestorTitle().with(
                    vertParentTitle().with(
                        div().withClass("tile is-child")
                            .with(urlContent)
                            .with(convertNested(el))));
            }
        });
        htmlRenderer.put(RbelBearerTokenElement.class, (el, key) -> {
            final ContainerTag tokenContent = div(text(performElementToTextConversion(el, key)));
            if (((RbelBearerTokenElement) el).getBearerToken() instanceof RbelStringElement) {
                return tokenContent;
            } else {
                return ancestorTitle().with(
                    vertParentTitle().with(
                        div().withClass("tile is-child")
                            .with(tokenContent)
                            .with(convertNested(el))));
            }
        });
        htmlRenderer.put(RbelStringElement.class, (el, key) -> div(text(performElementToTextConversion(el, key))));
    }

    private static EmptyTag link2CSS(final String url) {
        return link().attr("rel", "stylesheet")
            .withHref(url);
    }

    private static ContainerTag ancestorTitle() {
        return div().withClass("tile is-ancestor");
    }

    private static ContainerTag vertParentTitle() {
        return div().withClass("tile is-vertical is-parent");
    }

    private static ContainerTag childBoxNotifTitle(final String addClasses) {
        return div().withClass("tile is-child box notification " + addClasses);
    }

    private static ContainerTag t1ms(final String text) {
        return h1(text).withClass("is-family-monospace title");
    }

    private static ContainerTag t2(final String text) {
        return h2(text).withClass("title");
    }

    public static String render(final List<RbelElement> elements) {
        return render(elements, new RbelValueShader());
    }

    public static String render(final List<RbelElement> elements, final RbelValueShader valueShader) {
        return new RbelHtmlRenderer(valueShader)
            .performRendering(elements);
    }

    public String doRender(final List<RbelElement> elements) {
        return performRendering(elements);
    }

    private static DomContent renderMenu(final List<RbelElement> elements) {
        return div().withClass(" column is-one-fifth menu is-size-4 sidebar").with(
            a(i().withClass("fas fa-angle-double-up")).withId("collapse-all").withHref("#")
                .withClass("is-pulled-right mr-3"),
            a(i().withClass("fas fa-angle-double-down")).withId("expand-all").withHref("#")
                .withClass("is-pulled-right mr-3"),
            h2("Flow").withClass("mb-4 ml-2"),
            div().withClass("ml-5").with(
                elements.stream()
                    .map(RbelHtmlRenderer::menuTab)
                    .collect(Collectors.toList())
            )
        );
    }

    private static void initializeElementIndexMap(final List<RbelElement> elements) {
        IntStream.range(0, elements.size())
            .forEach(i -> elementIndices.put(elements.get(i).getUUID(), String.valueOf(i + 1)));
    }

    private static DomContent menuTab(final RbelElement element) {
        if (element instanceof RbelHttpRequest) {
            return div().withClass("ml-5").with(
                a().withHref("#" + element.getUUID()).withClass("mt-3 is-block").with(
                    div(span(elementIndices.get(element.getUUID())).withClass("tag is-info is-light mr-1"),
                        i().withClass("fas fa-share"),
                        text(" REQUEST")).withClass("menu-label mb-1 has-text-link"),
                    div(((RbelHttpRequest) element).getMethod() + "  " + ((RbelHttpRequest) element).getPath()
                        .getOriginalUrl()).attr("style", "text-overflow: ellipsis;overflow: hidden;")
                        .withClass("is-size-6 ml-3")
                )
            );
        } else {
            return a(span(elementIndices.get(element.getUUID())).withClass("tag is-info is-light mr-1"),
                i().withClass("fas fa-reply"),
                text(" RESPONSE"))
                .withHref("#" + element.getUUID()).withClass("menu-label ml-5 mt-3 is-block has-text-success");
        }
    }

    private JsonElement shadeJson(final JsonElement input, final Optional<String> key) {
        if (input.isJsonPrimitive()) {
            if (key.isPresent() && rbelValueShader.shouldConvert(key.get())) {
                return new JsonPrimitive(rbelValueShader.convert(key.get(), input.getAsJsonPrimitive().getAsString()));
            } else {
                return input;
            }
        } else if (input.isJsonObject()) {
            final JsonObject output = new JsonObject();
            for (final Entry<String, JsonElement> element : input.getAsJsonObject().entrySet()) {
                output.add(element.getKey(), shadeJson(element.getValue(), Optional.of(element.getKey())));
            }
            return output;
        } else if (input.isJsonArray()) {
            final JsonArray output = new JsonArray();
            for (final JsonElement element : input.getAsJsonArray()) {
                output.add(shadeJson(element, Optional.empty()));
            }
            return output;
        } else if (input.isJsonNull()) {
            return input;
        } else {
            throw new RuntimeException("Unshadeable JSON-Type " + input.getClass().getSimpleName());
        }
    }

    private String performElementToTextConversion(final RbelElement el, final Optional<String> key) {
        return key
            .map(keyValue -> rbelValueShader.convert(keyValue, el.getContent()))
            .orElse(el.getContent())
            .replace("\n", "<br/>");
    }

    private ContainerTag collapsibleCard(final ContainerTag title, final ContainerTag body) {
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
                                div()//.withClass("content")
                                    .with(body)
                            )
                    )
            );
    }

    private ContainerTag renderUrlContent(final RbelElement el) {
        final String originalUrl = ((RbelPathElement) el).getOriginalUrl();
        return div(new UnescapedText(originalUrl
            .replace("?", "?<br/>")
            .replace("&", "<br/>&")));
    }

    private List<ContainerTag> convertNested(final RbelElement el) {
        return
            el.traverseAndReturnNestedMembers().entrySet().stream()
                .filter(child -> !(child.getValue() instanceof RbelStringElement))
                .map(child ->
                    article().withClass("tile is-ancestor notification is-warning my-6")
                        .with(
                            h2(child.getKey()).withClass("title").withStyle("word-break: keep-all;"),
                            div(
                                convert(child.getValue(), Optional.ofNullable(child.getKey()))
                                    .withClass("notification tile is-child box")
                            ).withClass("notification tile is-parent")
                        )
                )
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private synchronized String performRendering(final List<RbelElement> elements) {

        initializeElementIndexMap(elements);

        return TagCreator.document(
            html(
                head(
                    meta().attr("charset", "utf-8"),
                    meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"),
                    title().withText("Rbel Flow"),
                    link2CSS("https://cdn.jsdelivr.net/npm/bulma@0.9.1/css/bulma.min.css"),
                    link2CSS("https://jenil.github.io/bulmaswatch/simplex/bulmaswatch.min.css"),
                    link2CSS("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.2/css/all.min.css"),
                    tag("style").with(
                        new UnescapedText(IOUtils.resourceToString("/rbel.css", StandardCharsets.UTF_8)))
                ),
                body()
                    .with(
                        section().withClass("main-content").with(
                            section().withClass("columns is-vcentered header").with(
                                div().withClass("column is-one-fifth is-inline-block logo").with(
                                    img().withSrc("https://colinbeavan.com/wp-content/uploads/2018/05/backwards.jpg")
                                ),
                                div().withClass("is-inline-block").with(
                                    h1(title).withClass("is-size-1 mb-3"),
                                    div(new UnescapedText(subTitle)).withClass("is-size-6 is-italic is-clearfix")
                                )
                            ),
                            section().withClass("columns is-fullheight").with(
                                renderMenu(elements),
                                div().withClass("column m-6").with(
                                    div("Created " +
                                        DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()))
                                        .withClass("is-italic is-size-6 is-pulled-right mr-6"),
                                    div().with(
                                        elements.stream()
                                            .map(el -> convert(el, Optional.empty()))
                                            .collect(Collectors.toList())
                                    ),
                                    div("Created " +
                                        DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()))
                                        .withClass("is-italic is-size-6 is-pulled-right mr-6")
                                )
                            )
                        )
                    ).with(
                    script().with(new UnescapedText(IOUtils.resourceToString("/rbel.js", StandardCharsets.UTF_8)))
                )
            )
        );
    }

    private ContainerTag convert(final RbelElement element, final Optional<String> key) {
        if (htmlRenderer.containsKey(element.getClass())) {
            return htmlRenderer.get(element.getClass()).apply(element, key);
        } else {
            return p().withText(element.getClass().getSimpleName() + "<br/> " + element.getContent());
        }
    }
}

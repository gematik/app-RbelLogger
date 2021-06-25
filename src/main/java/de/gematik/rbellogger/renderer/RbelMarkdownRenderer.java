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

package de.gematik.rbellogger.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.gematik.rbellogger.data.elements.*;
import java.util.stream.Collectors;

public class RbelMarkdownRenderer {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static String render(final RbelElement element) {
        return render(element, 0);
    }

    public static String render(final RbelElement element, int depth) {
        if (element instanceof RbelHttpResponse) {
            return render((RbelHttpResponse) element, depth + 1);
        }
        if (element instanceof RbelHttpRequest) {
            return render((RbelHttpRequest) element, depth + 1);
        }
        if (element instanceof RbelMapElement) {
            return render((RbelMapElement) element, depth + 1);
        }
        if (element instanceof RbelStringElement) {
            return element.getContent();
        }
        if (element instanceof RbelUriElement) {
            return render((RbelUriElement) element, depth + 1);
        }
        if (element instanceof RbelJsonElement) {
            return render((RbelJsonElement) element, depth + 1);
        }
        if (element instanceof RbelJwtElement) {
            return render((RbelJwtElement) element, depth + 1);
        }
        if (element instanceof RbelJweElement) {
            return render((RbelJweElement) element, depth + 1);
        }
        return element.getContent();
    }

    public static String render(final RbelHttpResponse message, int depth) {
        final StringBuilder builder = new StringBuilder().append("#".repeat(depth)).append(" Response\n")
            .append("```\n")
            .append(message.getResponseCode()).append("\n\n").append(render(message.getHeader(), depth))
            .append("\n```\n\n");
        if (message.getBody() != null && !(message.getBody() instanceof RbelNullElement)) {
            builder.append(render(message.getBody(), depth));
        }
        return builder.toString();
    }

    public static String render(final RbelHttpRequest message, int depth) {
        final StringBuilder builder = new StringBuilder().append("#".repeat(depth)).append(" Request\n").append("```\n")
            .append(message.getMethod()).append(" ").append(render(message.getPath())).append("\n\n")
            .append(render(message.getHeader(), depth))
            .append("\n```\n\n");
        if (message.getBody() != null && !(message.getBody() instanceof RbelNullElement)) {
            builder.append(render(message.getBody(), depth));
        }
        return builder.toString();
    }

    public static String render(final RbelJsonElement json, int depth) {
        return "```\n" + GSON.toJson(JsonParser.parseString(json.getCompleteJsonString())) + "\n```";
    }

    public static String render(final RbelUriElement pathElement, int depth) {
        return pathElement.getOriginalUrl()
            .replace("?", "\n?")
            .replace("&", "\n&");
    }

    public static String render(final RbelJwtSignature signature, int depth) {
        return "```\n" + GSON.toJson(signature) + "\n```";
    }

    public static String render(final RbelJwtElement jwt, int depth) {
        return "#".repeat(depth) + " JWT\n" +
            "#".repeat(depth + 1) + "Header:\n" + render(jwt.getHeader(), depth)
            + "\n\n" + "#".repeat(depth + 1) + "Body:\n" + render(jwt.getBody(), depth)
            + "\n\n" + "#".repeat(depth + 1) + "Signature:\n" + render(jwt.getSignature(), depth);
    }

    public static String render(final RbelJweElement jwe, int depth) {
        return "#".repeat(depth) + " JWE\n" +
            "#".repeat(depth + 1) + "Header:\n" + render(jwe.getHeader(), depth)
            + "\n\n" + "#".repeat(depth + 1) + "Body:\n" + render(jwe.getBody(), depth)
            + "\n\n" + "#".repeat(depth + 1) + "Decryption:\n" + render(jwe.getEncryptionInfo(), depth);
    }

    public static String render(final RbelMapElement map, int depth) {
        return map.getElementMap().entrySet().stream()
            .map(entry -> entry.getKey() + ": " + render(entry.getValue(), depth))
            .collect(Collectors.joining("\n"));
    }
}

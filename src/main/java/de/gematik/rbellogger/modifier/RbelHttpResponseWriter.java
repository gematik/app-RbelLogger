package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpHeaderFacet;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class RbelHttpResponseWriter implements RbelElementWriter {
    @Override
    public boolean canWrite(RbelElement oldTargetElement) {
        return oldTargetElement.hasFacet(RbelHttpResponseFacet.class)
            || oldTargetElement.hasFacet(RbelHttpRequestFacet.class);
    }

    @Override
    public byte[] write(RbelElement oldTargetElement, RbelElement oldTargetModifiedChild, byte[] newContent) {
        final Optional<RbelHttpResponseFacet> responseFacet = oldTargetElement.getFacet(RbelHttpResponseFacet.class);
        final Optional<RbelHttpRequestFacet> requestFacet = oldTargetElement.getFacet(RbelHttpRequestFacet.class);
        final RbelHttpMessageFacet messageFacet = oldTargetElement.getFacetOrFail(RbelHttpMessageFacet.class);
        final StringJoiner joiner = new StringJoiner("\r\n");

        joiner.add(buildTitleLine(oldTargetModifiedChild, new String(newContent), responseFacet, requestFacet));

        byte[] body = getChunkedMapper(oldTargetElement)
            .apply(getBodyContent(messageFacet, oldTargetModifiedChild, newContent));
        if (messageFacet.getHeader() == oldTargetModifiedChild) {
            joiner.add(new String(newContent));
        } else {
            joiner.add(patchHeader(new String(messageFacet.getHeader().getRawContent()), body.length));
        }
        joiner.add("");
        joiner.add("");
        return ArrayUtils.addAll(joiner.toString().getBytes(StandardCharsets.UTF_8),
            body);
    }

    private UnaryOperator<byte[]> getChunkedMapper(RbelElement oldTargetElement) {
        if (isChunkedMessage(oldTargetElement)) {
            return array -> ArrayUtils.addAll((array.length + "\r\n").getBytes(oldTargetElement.getElementCharset()),
                ArrayUtils.addAll(array, ("\r\n0\r\n").getBytes(oldTargetElement.getElementCharset())));
        } else {
            return UnaryOperator.identity();
        }
    }

    private boolean isChunkedMessage(RbelElement oldTargetElement) {
        return oldTargetElement.getFacet(RbelHttpMessageFacet.class)
            .map(RbelHttpMessageFacet::getHeader)
            .flatMap(el -> el.getFacet(RbelHttpHeaderFacet.class))
            .stream()
            .flatMap(h -> h.getCaseInsensitiveMatches("Transfer-Encoding"))
            .map(RbelElement::getRawStringContent)
            .filter(value -> value.equalsIgnoreCase("chunked"))
            .findAny().isPresent();
    }

    private String patchHeader(String headerRaw, int length) {
        return Arrays.stream(headerRaw.split("\r\n"))
            .map(headerLine -> {
                if (headerLine.toLowerCase(Locale.ROOT).startsWith("content-length")) {
                    return "Content-Length: " + length;
                } else {
                    return headerLine;
                }
            })
            .collect(Collectors.joining("\r\n"));
    }

    private byte[] getBodyContent(RbelHttpMessageFacet messageFacet, RbelElement oldTargetModifiedChild, byte[] newContent) {
        if (messageFacet.getBody() == oldTargetModifiedChild) {
            return newContent;
        } else {
            return messageFacet.getBody().getRawContent();
        }
    }

    private String buildTitleLine(RbelElement oldTargetModifiedChild, String newContent,
                                  Optional<RbelHttpResponseFacet> responseFacet,
                                  Optional<RbelHttpRequestFacet> requestFacet) {
        StringBuilder builder = new StringBuilder();
        if (requestFacet.isPresent()) {
            if (requestFacet.get().getMethod() == oldTargetModifiedChild) {
                builder.append(newContent);
            } else {
                builder.append(requestFacet.get().getMethod().getRawStringContent());
            }
            builder.append(" ");
            if (requestFacet.get().getPath() == oldTargetModifiedChild) {
                builder.append(newContent);
            } else {
                builder.append(requestFacet.get().getPath().getRawStringContent());
            }
            builder.append(" HTTP/1.1");
            return builder.toString();
        }

        if (responseFacet.get().getResponseCode() == oldTargetModifiedChild) {
            return "HTTP/1.1 " + newContent;
        } else {
            return "HTTP/1.1 " + responseFacet.get().getResponseCode().getRawStringContent();
        }
    }
}

package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class RbelHttpResponseWriter implements RbelElementWriter {
    @Override
    public boolean canWrite(RbelElement oldTargetElement) {
        return oldTargetElement.hasFacet(RbelHttpResponseFacet.class)
            || oldTargetElement.hasFacet(RbelHttpRequestFacet.class);
    }

    @Override
    public String write(RbelElement oldTargetElement, RbelElement oldTargetModifiedChild, String newContent) {
        final Optional<RbelHttpResponseFacet> responseFacet = oldTargetElement.getFacet(RbelHttpResponseFacet.class);
        final Optional<RbelHttpRequestFacet> requestFacet = oldTargetElement.getFacet(RbelHttpRequestFacet.class);
        final RbelHttpMessageFacet messageFacet = oldTargetElement.getFacetOrFail(RbelHttpMessageFacet.class);
        final StringJoiner joiner = new StringJoiner("\r\n");

        joiner.add(buildTitleLine(oldTargetModifiedChild, newContent, responseFacet, requestFacet));

        String body = getBodyString(messageFacet, oldTargetModifiedChild, newContent);
        if (messageFacet.getHeader() == oldTargetModifiedChild) {
            joiner.add(newContent);
        } else {
            joiner.add(
                patchHeader(messageFacet.getHeader().getRawStringContent(), body.getBytes().length));
        }
        joiner.add("");
        joiner.add(body);
        return joiner.toString();
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

    private String getBodyString(RbelHttpMessageFacet messageFacet, RbelElement oldTargetModifiedChild, String newContent) {
        if (messageFacet.getBody() == oldTargetModifiedChild) {
            return newContent;
        } else {
            return messageFacet.getBody().getRawStringContent();
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

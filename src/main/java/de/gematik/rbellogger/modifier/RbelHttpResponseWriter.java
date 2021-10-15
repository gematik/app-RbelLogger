package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import de.gematik.rbellogger.data.facet.RbelHttpResponseFacet;

import java.util.Optional;
import java.util.StringJoiner;

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

        if (messageFacet.getHeader() == oldTargetModifiedChild) {
            joiner.add(newContent);
        } else {
            joiner.add(messageFacet.getHeader().getRawStringContent());
        }
        joiner.add("");
        if (messageFacet.getBody() == oldTargetModifiedChild) {
            joiner.add(newContent);
        } else {
            joiner.add(messageFacet.getBody().getRawStringContent());
        }
        return joiner.toString();
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

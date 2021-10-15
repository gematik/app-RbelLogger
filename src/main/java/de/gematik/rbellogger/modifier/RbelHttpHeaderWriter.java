package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpHeaderFacet;

import java.util.Map;
import java.util.StringJoiner;

public class RbelHttpHeaderWriter implements RbelElementWriter {
    @Override
    public boolean canWrite(RbelElement oldTargetElement) {
        return oldTargetElement.hasFacet(RbelHttpHeaderFacet.class);
    }

    @Override
    public String write(RbelElement oldTargetElement, RbelElement oldTargetModifiedChild, String newContent) {
        StringJoiner joiner = new StringJoiner("\r\n");
        for (Map.Entry<String, RbelElement> entry : oldTargetElement.getFacetOrFail(RbelHttpHeaderFacet.class).entrySet()) {
            if (entry.getValue() == oldTargetModifiedChild) {
                joiner.add(entry.getKey() + ": " + newContent);
            } else {
                joiner.add(entry.getKey() + ": " + entry.getValue().getRawStringContent());
            }
        }
        return joiner.toString();
    }
}

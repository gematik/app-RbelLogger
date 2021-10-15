package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.data.RbelElement;

public interface RbelElementWriter {
    boolean canWrite(RbelElement oldTargetElement);

    String write(RbelElement oldTargetElement, RbelElement oldTargetModifiedChild, String newContent);
}

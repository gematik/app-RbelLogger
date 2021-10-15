package de.gematik.rbellogger.modifier;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data
@RequiredArgsConstructor
@Builder(toBuilder = true)
public class RbelModificationDescription {

    @With
    private final String name;
    @With
    private final String condition;
    @With
    private final String targetElement;
    @With
    private final String replaceWith;
    @With
    private final String regexFilter;
}

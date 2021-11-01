package de.gematik.rbellogger.modifier;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RbelModificationDescription {

    @With
    private String name;
    @With
    private String condition;
    @With
    private String targetElement;
    @With
    private String replaceWith;
    @With
    private String regexFilter;
}

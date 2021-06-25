package de.gematik.rbellogger.data.elements;

import lombok.Data;

@Data
public class RbelBinaryElement extends RbelNestedElement {

    private final byte[] rawData;

    public RbelBinaryElement(byte[] bytes) {
        super();
        rawData = bytes;
    }

    @Override
    public String getContent() {
        return new String(rawData);
    }
}

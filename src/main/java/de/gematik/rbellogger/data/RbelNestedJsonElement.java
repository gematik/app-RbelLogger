package de.gematik.rbellogger.data;

import java.util.List;
import lombok.Data;

@Data
public class RbelNestedJsonElement extends RbelElement {

    private final RbelElement data;

    public RbelNestedJsonElement(RbelElement data) {
        super();
        this.data = data;
    }

    @Override
    public List<? extends RbelElement> getChildNodes() {
        return List.of(data);
    }

    public boolean isNestedBoundary() {
        return false;
    }

    @Override
    public String getContent() {
        return data.getContent();
    }
}

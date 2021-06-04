package de.gematik.rbellogger.data;

import de.gematik.rbellogger.converter.RbelConverter;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public abstract class RbelNestedElement extends RbelElement {

    private RbelElement nestedElement;

    @Override
    public List<? extends RbelElement> getChildNodes() {
        if (nestedElement != null) {
            return nestedElement.getChildNodes();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        if (nestedElement != null) {
            return nestedElement.getChildElements();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setParentNode(RbelElement parentNode) {
        super.setParentNode(parentNode);
        if (nestedElement != null) {
            nestedElement.setParentNode(parentNode);
        }
    }

    @Override
    public boolean isSimpleElement() {
        return nestedElement == null;
    }
}

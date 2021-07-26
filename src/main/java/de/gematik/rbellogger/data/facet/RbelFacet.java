package de.gematik.rbellogger.data.facet;

import de.gematik.rbellogger.data.RbelElement;
import java.util.List;
import java.util.Map.Entry;

public interface RbelFacet {

    List<Entry<String, RbelElement>> getChildElements();
}

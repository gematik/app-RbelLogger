package de.gematik.rbellogger.renderer;

import de.gematik.rbellogger.data.elements.RbelElement;
import java.util.List;

public abstract class RbelRenderer {

    public abstract String performRendering(final List<RbelElement> elements);
}

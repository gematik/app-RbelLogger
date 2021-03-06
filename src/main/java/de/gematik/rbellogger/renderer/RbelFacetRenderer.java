package de.gematik.rbellogger.renderer;

import de.gematik.rbellogger.data.RbelElement;
import j2html.tags.ContainerTag;
import java.util.Optional;

public interface RbelFacetRenderer {

    boolean checkForRendering(RbelElement element);

    ContainerTag performRendering(RbelElement element, Optional<String> key, RbelHtmlRenderingToolkit renderingToolkit);
}

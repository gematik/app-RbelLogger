/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.data.facet;

import com.google.gson.JsonParser;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.renderer.RbelHtmlFacetRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit;
import de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.JsonNoteEntry;
import j2html.tags.ContainerTag;
import j2html.tags.UnescapedText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.ancestorTitle;
import static de.gematik.rbellogger.renderer.RbelHtmlRenderingToolkit.vertParentTitle;
import static j2html.TagCreator.div;
import static j2html.TagCreator.pre;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class RbelNoteFacet implements RbelFacet {
    private final String value;
    private final NoteStyling style;

    public RbelNoteFacet(String value) {
        this.value = value;
        this.style = NoteStyling.INFO;
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        return List.of();
    }

    @RequiredArgsConstructor
    public enum NoteStyling {
        INFO("has-text-info"),
        WARN("has-text-warning"),
        ERROR("has-text-danger");

        private final String cssClass;

        public String toCssClass() {
            return cssClass;
        }
    }
}

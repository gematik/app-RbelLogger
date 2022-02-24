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

package de.gematik.rbellogger.data.util;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.util.RbelAnsiColors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static de.gematik.rbellogger.RbelOptions.ACTIVATE_FACETS_PRINTING;
import static de.gematik.rbellogger.RbelOptions.RBEL_PATH_TREE_VIEW_VALUE_OUTPUT_LENGTH;
import static de.gematik.rbellogger.util.RbelAnsiColors.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RbelElementTreePrinter {

    private final RbelElement rootElement;
    @Builder.Default
    private final int maximumLevels = Integer.MAX_VALUE;
    @Builder.Default
    private final boolean printContent = true;
    @Builder.Default
    private final boolean printKeys = false;
    @Builder.Default
    private final boolean printFacets = true;

    public String execute() {
        String result = RED_BOLD + rootElement.getKey().orElse("") + RESET;
        result += printKeyOf(rootElement) + "\n";
        result += executeRecursive(rootElement, "", maximumLevels);
        return result;
    }

    private String executeRecursive(RbelElement position, String padding, int remainingLevels) {
        if (remainingLevels <= 0) {
            return "";
        }
        String result = "";
        for (Iterator<Map.Entry<String, RbelElement>> iterator = position.getChildNodesWithKey().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, RbelElement> childNode = iterator.next();
            String switchString, padString;
            if (iterator.hasNext()) {
                switchString = "├──";
                padString = "|  ";
            } else {
                switchString = "└──";
                padString = "   ";
            }
            // the tree structure
            result += YELLOW_BRIGHT + padding + switchString + RESET;
            // name of the node
            result += RbelAnsiColors.RED_BOLD + childNode.getKey() + RbelAnsiColors.RESET;
            // print content
            result += printContentOf(childNode.getValue());
            // print facet
            result += printFacets(childNode.getValue());
            result += "\n";
            if (!childNode.getValue().getChildNodes().isEmpty()) {
                result += executeRecursive(childNode.getValue(), padding + padString, remainingLevels - 1);
            }
        }
        return result;
    }

    private String printFacets(RbelElement value) {
        if (!ACTIVATE_FACETS_PRINTING) {
            return "";
        }
        final String facetsString = value.getFacets().stream()
            .map(Object::getClass)
            .map(Class::getSimpleName)
            .filter(s -> !"RbelRootFacet".equals(s))
            .filter(s -> !"RbelListFacet".equals(s))
            .filter(s -> !"RbelNestedFacet".equals(s))
            .filter(s -> !"RbelMapFacet".equals(s))
            .collect(Collectors.joining(","));
        if (StringUtils.isEmpty(facetsString)) {
            return "";
        }
        return CYAN + " (" + facetsString + ")" + RESET;
    }

    private String printKeyOf(RbelElement value) {
        if (!printKeys) {
            return "";
        }
        return " " + GREEN + "[$." + value.findNodePath() + "]" + RESET;
    }

    private String printContentOf(RbelElement value) {
        if (!printContent) {
            return "";
        }
        String content = value.getRawStringContent();
        if (content == null) {
            content = value.seekValue()
                .map(Object::toString)
                .map(strValue -> "Value: " + strValue)
                .orElse("<null>");
        }
        if (content == null) {
            return "";
        }
        return " (" + RbelAnsiColors.BLUE + StringUtils.substring(content
            .replace("\n", "\\n")
            .replace("\r", "\\r"), 0, RBEL_PATH_TREE_VIEW_VALUE_OUTPUT_LENGTH)
            + (content.length() > RBEL_PATH_TREE_VIEW_VALUE_OUTPUT_LENGTH ? "..." : "")
            + RbelAnsiColors.RESET + ")";
    }
}

package de.gematik.rbellogger.util;

import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelJsonFacet;
import de.gematik.rbellogger.data.facet.RbelNestedFacet;
import de.gematik.rbellogger.data.facet.RbelValueFacet;
import de.gematik.rbellogger.exceptions.RbelPathException;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RbelPathExecutor {

    private final RbelElement rbelElement;
    private final String rbelPath;

    private static List<RbelElement> findAllChildsRecursive(final RbelElement element) {
        final List<? extends RbelElement> childNodes = element.getChildNodes();
        List<RbelElement> result = new ArrayList<>(childNodes);
        childNodes.stream()
            .map(RbelPathExecutor::findAllChildsRecursive)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .forEach(result::add);
        return result;
    }

    public List<RbelElement> execute() {
        if (!rbelPath.startsWith("$")) {
            throw new RbelPathException("RbelPath expressions always start with $.");
        }
        final List<String> keys = List.of(rbelPath.substring(2).split("\\.(?![^\\(]*\\))"));
        List<RbelElement> candidates = List.of(rbelElement);
        for (String key : keys) {
            candidates = candidates.stream()
                .map(element -> resolveRbelPathElement(key, element))
                .flatMap(List::stream)
                .map(this::descendToContentIfJsonChild)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
        }

        return candidates.stream()
            .filter(el -> !(el.hasFacet(RbelJsonFacet.class) && el.hasFacet(RbelNestedFacet.class)))
            .collect(Collectors.toUnmodifiableList());
    }

    private List<RbelElement> descendToContentIfJsonChild(RbelElement rbelElement) {
        if (rbelElement.hasFacet(RbelJsonFacet.class)
            && rbelElement.hasFacet(RbelNestedFacet.class)) {
            return List.of(rbelElement.getFacet(RbelNestedFacet.class)
                    .map(RbelNestedFacet::getNestedElement)
                    .get(),
                rbelElement);
        } else {
            return List.of(rbelElement);
        }
    }

    private List<? extends RbelElement> resolveRbelPathElement(final String key, final RbelElement element) {
        if (key.startsWith("[") && key.endsWith("]")) {
            return executeFunctionalExpression(key.substring(1, key.length() - 1).trim(), element);
        } else if (key.contains("[") && key.endsWith("]")) {
            final String[] parts = key.split("\\[");
            final String subKey = parts[0];
            List<? extends RbelElement> keySelectionResult = executeNonFunctionalExpression(subKey, element);
            final int selectionIndex = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1));
            if (keySelectionResult.size() <= selectionIndex) {
                return Collections.emptyList();
            }
            return List.of(keySelectionResult.get(selectionIndex));
        } else {
            return executeNonFunctionalExpression(key, element);
        }
    }

    private List<? extends RbelElement> executeNonFunctionalExpression(String key, RbelElement element) {
        if (key.isEmpty()) {
            return findAllChildsRecursive(element);
        } else if (key.equals("*")) {
            return element.getChildNodes();
        } else {
            return element.getAll(key);
        }
    }

    private List<? extends RbelElement> executeFunctionalExpression(
        final String functionExpression, final RbelElement element) {
        if (functionExpression.startsWith("'") && functionExpression.endsWith("'")) {
            return element.getAll(functionExpression.substring(1, functionExpression.length() - 1));
        } else if (functionExpression.equals("*")) {
            return element.getChildNodes();
        } else if (functionExpression.startsWith("?")) {
            if (functionExpression.startsWith("?(") && functionExpression.endsWith(")")) {
                return findChildNodesByJexlExpression(element,
                    functionExpression.substring(2, functionExpression.length() - 1));
            } else {
                throw new RbelPathException(
                    "Invalid JEXL-Expression encountered (Does not start with '?(' and end with ')'): "
                        + functionExpression);
            }
        } else {
            throw new RbelPathException("Unknown function expression encountered: " + functionExpression);
        }
    }

    private List<RbelElement> findChildNodesByJexlExpression(final RbelElement element, final String jexl) {
        RbelJexlExecutor executor = new RbelJexlExecutor();
        return element.getChildNodesWithKey().stream()
            .filter(candidate ->
                executor.matchesAsJexlExpression(candidate.getValue(), jexl, Optional.of(candidate.getKey())))
            .map(Entry::getValue)
            .collect(Collectors.toList());
    }
}

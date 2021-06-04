package de.gematik.rbellogger.util;

import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelNestedElement;
import de.gematik.rbellogger.exceptions.RbelPathException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RbelPathExecutor {

    private final RbelElement rbelElement;
    private final String rbelPath;

    private static boolean tryToParseInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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
                .distinct()
                .collect(Collectors.toList());
        }
        // never let the expression end on a basic container with a nested Element inside
        return candidates.stream()
            .map(candidate -> {
                if ((candidate instanceof RbelNestedElement)
                && (((RbelNestedElement) candidate).getNestedElement() != null)){
                    return ((RbelNestedElement) candidate).getNestedElement();
                } else {
                    return candidate;
                }
            })
            .collect(Collectors.toList());
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
        return element.getChildElements().stream()
            .filter(candidate ->
                executor.matchesAsJexlExpression(candidate.getValue(), jexl, Optional.of(candidate.getKey())))
            .map(Entry::getValue)
            .collect(Collectors.toList());
    }
}

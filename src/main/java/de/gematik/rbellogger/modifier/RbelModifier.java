package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKeyManager;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Builder
public class RbelModifier {

    private final RbelKeyManager rbelKeyManager;
    private final RbelConverter rbelConverter;
    private final List<RbelElementWriter> elementWriterList = new ArrayList<>(List.of(
        new RbelHttpHeaderWriter(),
        new RbelHttpResponseWriter(),
        new RbelJsonWriter()
    ));
    private final Map<String, RbelModificationDescription> modificationsMap = new HashMap<>();

    public RbelElement applyModifications(final RbelElement message) {
        RbelElement modifiedMessage = message;
        for (RbelModificationDescription modification : modificationsMap.values()) {
            if (shouldBeApplied(modification, message)) {
                modifiedMessage = rbelConverter.convertElement(applyModification(modification, modifiedMessage), null);
            }
        }
        return modifiedMessage;
    }

    private boolean shouldBeApplied(RbelModificationDescription modification, RbelElement message) {
        if (StringUtils.isEmpty(modification.getCondition())) {
            return true;
        }
        RbelJexlExecutor executor = new RbelJexlExecutor();
        return executor.matchesAsJexlExpression(message, modification.getCondition(), Optional.empty());
    }

    private String applyModification(RbelModificationDescription modification, RbelElement message) {
        RbelElement targetElement = message.findElement(modification.getTargetElement())
            .orElseThrow(() -> new RbelModificationException("Could not find element " + modification.getTargetElement() + " in message!"));

        RbelElement oldTargetElement = targetElement.getParentNode();
        RbelElement oldTargetModifiedChild = targetElement;
        String newContent = applyRegexAndReturnNewContent(targetElement, modification);
        while (oldTargetElement != null) {
            Optional<String> found = Optional.empty();
            for (RbelElementWriter writer : elementWriterList) {
                if (writer.canWrite(oldTargetElement)) {
                    String write = writer.write(oldTargetElement, oldTargetModifiedChild, newContent);
                    found = Optional.of(write);
                    break;
                }
            }
            if (found.isEmpty()) {
                throw new RbelModificationException("Could not rewrite element with facets " +
                    oldTargetElement.getFacets().stream()
                        .map(Object::getClass)
                        .map(Class::getSimpleName)
                        .collect(Collectors.toList()) + "!");
            }
            newContent = found.get();
            oldTargetModifiedChild = oldTargetElement;
            oldTargetElement = oldTargetElement.getParentNode();
        }
        return newContent;
    }

    private String applyRegexAndReturnNewContent(RbelElement targetElement, RbelModificationDescription modification) {
        if (StringUtils.isEmpty(modification.getRegexFilter())) {
            return modification.getReplaceWith();
        } else {
            return targetElement.getRawStringContent()
                .replaceAll(modification.getRegexFilter(), modification.getReplaceWith());
        }
    }

    public void deleteAllModifications() {
        modificationsMap.clear();
    }

    public void addModification(RbelModificationDescription modificationDescription) {
        modificationsMap.put(modificationDescription.getName(), modificationDescription);
    }

    private class RbelModificationException extends RuntimeException {
        public RbelModificationException(String s) {
            super(s);
        }
    }
}

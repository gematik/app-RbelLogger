package de.gematik.rbellogger.modifier;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelJexlExecutor;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKeyManager;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

public class RbelModifier {

    private final RbelKeyManager rbelKeyManager;
    private final RbelConverter rbelConverter;
    private final List<RbelElementWriter> elementWriterList;
    private final Map<String, RbelModificationDescription> modificationsMap = new HashMap<>();

    @Builder
    public RbelModifier(RbelKeyManager rbelKeyManager, RbelConverter rbelConverter) {
        this.rbelKeyManager = rbelKeyManager;
        this.rbelConverter = rbelConverter;
        this.elementWriterList = new ArrayList<>(List.of(
                new RbelHttpHeaderWriter(),
                new RbelHttpResponseWriter(),
                new RbelJsonWriter(),
                new RbelUriWriter(),
                new RbelUriParameterWriter(),
                new RbelJwtWriter(this.rbelKeyManager),
                new RbelJweWriter(this.rbelKeyManager)
        ));
    }

    public RbelElement applyModifications(final RbelElement message) {
        RbelElement modifiedMessage = message;
        for (RbelModificationDescription modification : modificationsMap.values()) {
            if (shouldBeApplied(modification, message)) {
                final Optional<RbelElement> targetOptional = modifiedMessage.findElement(modification.getTargetElement());
                if (targetOptional.isEmpty()) {
                    continue;
                }

                modifiedMessage = rbelConverter.convertElement(applyModification(modification, targetOptional.get()), null);
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

    private String applyModification(RbelModificationDescription modification, RbelElement targetElement) {
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
        if (StringUtils.isEmpty(modificationDescription.getName())) {
            modificationsMap.put(UUID.randomUUID().toString(), modificationDescription);
        } else {
            modificationsMap.put(modificationDescription.getName(), modificationDescription);
        }
    }

    public class RbelModificationException extends RuntimeException {
        public RbelModificationException(String s) {
            super(s);
        }
    }
}

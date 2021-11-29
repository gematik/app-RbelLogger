package de.gematik.rbellogger.modifier;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelKeyManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

public abstract class AbstractModifierTest {

    public static final RbelKeyFolderInitializer RBEL_KEY_FOLDER_INITIALIZER = new RbelKeyFolderInitializer(
        "src/test/resources");
    public RbelLogger rbelLogger;

    @BeforeEach
    public void initRbelLogger() {
        RbelOptions.activateJexlDebugging();
        if (rbelLogger == null) {
            rbelLogger = RbelLogger.build(
                getRbelConfiguration());
        }
        rbelLogger.getRbelModifier().deleteAllModifications();
    }

    public RbelConfiguration getRbelConfiguration() {
        return new RbelConfiguration();
    }

    public RbelElement modifyMessageAndParseResponse(RbelElement message) {
        final RbelElement modifiedMessage = rbelLogger.getRbelModifier().applyModifications(message);
        return modifiedMessage;
    }

    public RbelElement readAndConvertCurlMessage(String fileName, Function<String, String>... messageMappers)
        throws IOException {
        String curlMessage = readCurlFromFileWithCorrectedLineBreaks(fileName);
        for (Function<String, String> mapper : messageMappers) {
            curlMessage = mapper.apply(curlMessage);
        }
        return rbelLogger.getRbelConverter()
            .convertElement(curlMessage, null);
    }

    public void setKeyManagerAvailableKeys(List<RbelKey> keys) throws IllegalAccessException {
        Field list = ReflectionUtils.findFields(RbelKeyManager.class, f -> f.getName().equals("keyList"),
            HierarchyTraversalMode.BOTTOM_UP).get(0);
        list.setAccessible(true);
        list.set(rbelLogger.getRbelKeyManager(), keys);
    }
}

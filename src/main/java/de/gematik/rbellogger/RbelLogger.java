package de.gematik.rbellogger;

import de.gematik.rbellogger.captures.RbelCapturer;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.converter.listener.RbelX5cKeyReader;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelMapElement;
import de.gematik.rbellogger.key.RbelKeyManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class RbelLogger {

    private final RbelConverter rbelConverter;
    private final RbelCapturer rbelCapturer;
    private final RbelValueShader valueShader;
    private final RbelKeyManager rbelKeyManager;

    public static RbelLogger build() {
        return build(new RbelConfiguration());
    }

    public static RbelLogger build(final RbelConfiguration configuration) {
        Objects.requireNonNull(configuration);

        final RbelConverter rbelConverter = RbelConverter.builder()
            .rbelKeyManager(new RbelKeyManager())
            .rbelValueShader(new RbelValueShader())
            .build();
        rbelConverter.registerListener(RbelMapElement.class, new RbelX5cKeyReader());
        if (configuration.getPostConversionListener() != null) {
            configuration.getPostConversionListener().entrySet().stream()
                .forEach(entry -> entry.getValue().stream()
                    .forEach(listener -> rbelConverter.registerListener(entry.getKey(), listener)));
            rbelConverter.getPostConversionListener().putAll(configuration.getPostConversionListener());
        }
        if (configuration.getPreConversionMappers() != null) {
            configuration.getPreConversionMappers().entrySet().stream()
                .forEach(entry -> entry.getValue().stream()
                    .forEach(listener -> rbelConverter.registerMapper(entry.getKey(), listener)));
            rbelConverter.getPostConversionListener().putAll(configuration.getPostConversionListener());
        }
        rbelConverter.registerListener(RbelElement.class,
            rbelConverter.getRbelValueShader().getPostConversionListener());

        for (Consumer<RbelConverter> initializer : configuration.getInitializers()) {
            initializer.accept(rbelConverter);
        }
        rbelConverter.getRbelKeyManager().addAll(configuration.getKeys());
        if (configuration.getCapturer() != null) {
            configuration.getCapturer().setRbelConverter(rbelConverter);
            configuration.getCapturer().initialize();
        }

        return RbelLogger.builder()
            .rbelConverter(rbelConverter)
            .rbelCapturer(configuration.getCapturer())
            .rbelKeyManager(rbelConverter.getRbelKeyManager())
            .valueShader(rbelConverter.getRbelValueShader())
            .build();
    }

    public List<RbelElement> getMessageHistory() {
        return rbelConverter.getMessageHistory();
    }
}

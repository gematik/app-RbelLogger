package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelNestedJsonElement;
import de.gematik.rbellogger.data.RbelStringElement;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Optional;

public class RbelBase64JsonConverter extends RbelJsonConverter {

    @Override
    public boolean canConvertElement(RbelElement rbel, RbelConverter context) {
        return safeConvertBase64Using(rbel.getContent(), Base64.getDecoder(), context)
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getUrlDecoder(), context))
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getMimeDecoder(), context))
            .isPresent();
    }

    @Override
    public RbelElement convertElement(RbelElement rbel, RbelConverter context) {
        return safeConvertBase64Using(rbel.getContent(), Base64.getDecoder(), context)
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getUrlDecoder(), context))
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getMimeDecoder(), context))
            .orElseThrow();
    }

    private Optional<RbelElement> safeConvertBase64Using(String input, Decoder decoder, RbelConverter context) {
        try{
            return Optional.ofNullable(decoder.decode(input))
                .filter(json -> super.canConvertElement(new RbelStringElement(new String(json)), context))
                .map(json -> super.convertElement(new RbelStringElement(new String(json)), context))
                .map(rbelJsonElement -> new RbelNestedJsonElement(rbelJsonElement));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

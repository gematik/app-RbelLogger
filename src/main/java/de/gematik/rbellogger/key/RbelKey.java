package de.gematik.rbellogger.key;

import java.security.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RbelKey {

    public static int PRECEDENCE_X5C_HEADER_VALUE = 100;
    public static int PRECEDENCE_KEY_FOLDER = 110;

    private final Key key;
    private final String keyName;
    /**
     * The importance of this particular key. Higher value means it will be considered before potentially matching keys
     * with lower precedence.
     */
    private final int precedence;
}

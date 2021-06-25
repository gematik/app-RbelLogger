package de.gematik.rbellogger.data;

import de.gematik.rbellogger.exceptions.RbelConversionException;
import java.net.URI;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class RbelHostname {

    private final String hostname;
    private final int port;

    public String toString() {
        if (port > 0) {
            return hostname + ":" + port;
        } else {
            return hostname;
        }
    }

    public static RbelHostname generateFromUrl(String url) {
        try {
            final URI uri = new URI(url);
            if (uri.getPort() > 0) {
                return new RbelHostname(uri.getHost(), uri.getPort());
            } else if ("http".equals(uri.getScheme())) {
                return new RbelHostname(uri.getHost(), 80);
            } else if ("https".equals(uri.getScheme())) {
                return new RbelHostname(uri.getHost(), 443);
            } else {
                throw new RbelConversionException("Could not parse Hostname from '"+url+"'");
            }
        } catch (Exception e) {
            throw new RbelConversionException("Could not parse Hostname from '"+url+"'");
        }
    }
}

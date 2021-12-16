package de.gematik.rbellogger.data;

import de.gematik.rbellogger.exceptions.RbelConversionException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

@Data
@Builder
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class RbelHostname {

    private final String hostname;
    private final int port;

    public static RbelHostname fromString(final String value) {
        if (value.contains(":")) {
            try {
                return RbelHostname.builder()
                    .hostname(value.split(":")[0])
                    .port(Integer.parseInt(value.split(":")[1]))
                    .build();
            } catch (Exception e) {
                throw new RbelHostnameFormatException("Unable to parse hostname: '" + value + "'", e);
            }
        } else {
            return RbelHostname.builder()
                .hostname(value)
                .build();
        }
    }

    public static RbelHostname generateFromUrl(String url) {

        checkIfUrlIsNotEmpty(url);
        checkIfUrlIsValid(url);

        try {
            final URI uri = new URI(url);
            if (uri.getPort() > 0) {
                return new RbelHostname(uri.getHost(), uri.getPort());
            } else if ("http".equals(uri.getScheme())) {
                return new RbelHostname(uri.getHost(), 80);
            } else if ("https".equals(uri.getScheme())) {
                return new RbelHostname(uri.getHost(), 443);
            } else {
                throw new RbelConversionException("Could not parse Hostname from '" + url + "'");
            }
        } catch (Exception e) {
            throw new RbelConversionException("Could not parse Hostname from '" + url + "'");
        }
    }

    private static void checkIfUrlIsNotEmpty(String url) {
        if (StringUtils.isBlank(url)) {
            throw new RbelConversionException(
                "Could not parse Hostname, because there is none. Is there a configuration error?");
        }
    }

    private static void checkIfUrlIsValid(String url) {
        UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        if (!urlValidator.isValid(url)) {
            throw new RbelConversionException(
                "The given URL is invalid. Please check your configuration.");
        }
    }

    public String toString() {
        if (port > 0) {
            return hostname + ":" + port;
        } else {
            return hostname;
        }
    }

    private static class RbelHostnameFormatException extends RuntimeException {

        public RbelHostnameFormatException(String s, Exception e) {
            super(s, e);
        }
    }
}

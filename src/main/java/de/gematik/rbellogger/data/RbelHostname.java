package de.gematik.rbellogger.data;

import de.gematik.rbellogger.exceptions.RbelConversionException;
import de.gematik.rbellogger.exceptions.RbelHostnameFormatException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

@Data
@Builder
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class RbelHostname {

    private final String hostname;
    private final int port;

    public static Optional<RbelHostname> fromString(final String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        if (value.contains(":")) {
            String[] hostnameValues = value.split(":");
            int port = Integer.parseInt(hostnameValues[1]);

            checkIfHostnameIsValid(hostnameValues[0]);
            checkIfPortIsNegative(port);

            try {
                return Optional.ofNullable(RbelHostname.builder()
                    .hostname(hostnameValues[0])
                    .port(port)
                    .build());
            } catch (Exception e) {
                throw new RbelHostnameFormatException("Unable to parse hostname: '" + value + "'", e);
            }
        } else {
            checkIfHostnameIsValid(value);

            return Optional.ofNullable(RbelHostname.builder()
                .hostname(value)
                .build());
        }
    }

    private static void checkIfHostnameIsValid(String hostname) {
        if (hostname.contains(".")) {
            InetAddressValidator validator = InetAddressValidator.getInstance();
            if (!validator.isValid(hostname)) {
                throw new RbelHostnameFormatException(
                    "The given IP address is invalid. Please check your configuration.");
            }
        } else {
            DomainValidator validator = DomainValidator.getInstance(true);
            if (!validator.isValid(hostname)) {
                throw new RbelHostnameFormatException(
                    "The given hostname is invalid. Please check your configuration.");
            }
        }
    }

    private static void checkIfPortIsNegative(int port) {
        if (port < 0) {
            throw new RbelHostnameFormatException(
                "The given port '" + port + "' is invalid. Please check your configuration.");
        }
    }

    public static Optional<Object> generateFromUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return Optional.empty();
        }

        checkIfUrlIsValid(url);

        try {
            final URI uri = new URI(url);
            if (uri.getPort() > 0) {
                return Optional.of(new RbelHostname(uri.getHost(), uri.getPort()));
            } else if ("http".equals(uri.getScheme())) {
                return Optional.of(new RbelHostname(uri.getHost(), 80));
            } else if ("https".equals(uri.getScheme())) {
                return Optional.of(new RbelHostname(uri.getHost(), 443));
            } else {
                throw new RbelConversionException("Could not parse Hostname from '" + url + "'");
            }
        } catch (Exception e) {
            throw new RbelConversionException("Could not parse Hostname from '" + url + "'");
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
}

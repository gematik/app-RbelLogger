package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHttpRequestFacet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class RbelHttpRequestConverterTest {

    private RbelConverter rbelConverter = RbelLogger.build().getRbelConverter();

    @Test
    @DisplayName("should convert CURL request with defunct linebreaks")
    public void shouldConvertCurlRequestWithDefunctLinebreaks() {
        final RbelElement rbelElement = new RbelElement(("GET /auth/realms/idp/.well-known/openid-configuration HTTP/1.1\n"
                + "Accept: */*\n"
                + "Host: localhost:8080\n"
                + "Connection: Keep-Alive\n"
                + "User-Agent: Apache-HttpClient/4.5.12 (Java/11.0.8)\n"
                + "Accept-Encoding: gzip,deflate\n").getBytes(StandardCharsets.UTF_8), null);

        new RbelHttpRequestConverter().consumeElement(rbelElement, rbelConverter);

        assertThat(rbelElement.hasFacet(RbelHttpRequestFacet.class)).isTrue();
        assertThat(rbelElement.hasFacet(RbelHttpMessageFacet.class)).isTrue();
    }

    @Test
    public void doubleHeaderValue() {
        final RbelElement rbelElement = new RbelElement(("GET /auth/realms/idp/.well-known/openid-configuration HTTP/1.1\r\n"
                + "User-Agent: Value1\r\n"
                + "User-Agent: Value2\r\n\r\n").getBytes(StandardCharsets.UTF_8), null);

        new RbelHttpRequestConverter().consumeElement(rbelElement, rbelConverter);

        assertThat(rbelElement.hasFacet(RbelHttpRequestFacet.class)).isTrue();
    }

    @Test
    public void shouldNotConvertAcceptList() {
        final RbelElement rbelElement = new RbelElement(("GET,PUT,POST\"").getBytes(StandardCharsets.UTF_8), null);

        new RbelHttpRequestConverter().consumeElement(rbelElement, rbelConverter);

        assertThat(rbelElement.hasFacet(RbelHttpRequestFacet.class)).isFalse();
    }
}
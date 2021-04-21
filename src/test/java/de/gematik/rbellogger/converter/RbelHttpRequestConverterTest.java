package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.data.RbelStringElement;
import org.junit.jupiter.api.Test;

class RbelHttpRequestConverterTest {

    @Test
    public void shouldConvertCurlRequest() {
        assertThat(new RbelHttpRequestConverter()
            .canConvertElement(new RbelStringElement("GET /auth/realms/idp/.well-known/openid-configuration HTTP/1.1\n"
                + "Accept: */*\n"
                + "Host: localhost:8080\n"
                + "Connection: Keep-Alive\n"
                + "User-Agent: Apache-HttpClient/4.5.12 (Java/11.0.8)\n"
                + "Accept-Encoding: gzip,deflate\n"), null)
        ).isTrue();
    }

    @Test
    public void doubleHeaderValue() {
        assertThat(new RbelHttpRequestConverter()
            .convertElement(new RbelStringElement("GET /auth/realms/idp/.well-known/openid-configuration HTTP/1.1\n"
                + "User-Agent: Value1\n"
                + "User-Agent: Value2"), RbelConverter.builder().build())
        ).isNotNull();
    }

    @Test
    public void shouldNotConvertAcceptList() {
        assertThat(new RbelHttpRequestConverter()
            .canConvertElement(new RbelStringElement("GET,PUT,POST"), null)
        ).isFalse();
    }
}
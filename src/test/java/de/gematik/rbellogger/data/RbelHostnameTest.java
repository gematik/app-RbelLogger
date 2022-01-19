package de.gematik.rbellogger.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import de.gematik.rbellogger.exceptions.RbelConversionException;
import de.gematik.rbellogger.exceptions.RbelHostnameFormatException;
import org.junit.jupiter.api.Test;

public class RbelHostnameTest {

    @Test
    public void generateWithHttp_expectPort80() {
        assertThat(RbelHostname.generateFromUrl("http://foo")).get()
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 80);
    }

    @Test
    public void generateWithHttps_expectPort443() {
        assertThat(RbelHostname.generateFromUrl("https://foo")).get()
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 443);
    }

    @Test
    public void generateWithGivenPort_expectPortToBeReturned() {
        assertThat(RbelHostname.generateFromUrl("https://www.google.de:666")).get()
            .hasFieldOrPropertyWithValue("hostname", "www.google.de")
            .hasFieldOrPropertyWithValue("port", 666);
    }

    @Test
    public void generateWithGivenPortAndLocalUrl_expectPortToBeReturned() {
        assertThat(RbelHostname.generateFromUrl("https://foo:666")).get()
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 666);
    }

    @Test
    public void generateWithNoHostname_shouldFailWithException() {
        assertThat(RbelHostname.generateFromUrl(null))
            .isEmpty();
    }

    @Test
    public void generateWithEmptyHostname_shouldFailWithException() {
        assertThat(RbelHostname.generateFromUrl(""))
            .isEmpty();
    }

    @Test
    public void generateWithWrongScheme_shouldFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("httbs://foo:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithMissingScheme_shouldFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("foo:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithWrongHost_shouldFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("https://foo__:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithNegativePort_shouldFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("https://foo:-1");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateRbelHostnameFromString_withColon() {
        assertThat(RbelHostname.fromString("test:80")).get()
            .hasFieldOrPropertyWithValue("hostname", "test")
            .hasFieldOrPropertyWithValue("port", 80);
    }

    @Test
    public void generateRbelHostnameFromString_withoutColon() {
        assertThat(RbelHostname.fromString("test")).get()
            .hasFieldOrPropertyWithValue("hostname", "test")
            .hasFieldOrPropertyWithValue("port", 0);
    }

    @Test
    public void generateRbelHostnameFromString_withNumericalHostname() {
        assertThat(RbelHostname.fromString("127.0.0.1")).get()
            .hasFieldOrPropertyWithValue("hostname", "127.0.0.1")
            .hasFieldOrPropertyWithValue("port", 0);
    }

    @Test
    public void generateRbelHostnameFromString_withNegativePort() {
        assertThatExceptionOfType(RbelHostnameFormatException.class).isThrownBy(() -> {
            RbelHostname.fromString("test:-1");
        }).withMessage("The given port '-1' is invalid. Please check your configuration.");
    }

    @Test
    public void generateRbelHostnameFromString_emptyString() {
        assertThat(RbelHostname.fromString(""))
            .isEmpty();
    }

    @Test
    public void generateRbelHostnameFromString_noString() {
        assertThat(RbelHostname.fromString(null))
            .isEmpty();
    }

    @Test
    public void generateRbelHostnameFromString_invalidHostname() {
        assertThatExceptionOfType(RbelHostnameFormatException.class).isThrownBy(() -> {
            RbelHostname.fromString("test__");
        }).withMessage("The given hostname is invalid. Please check your configuration.");
    }

    @Test
    public void generateRbelHostnameFromString_invalidHostnameWithColon() {
        assertThatExceptionOfType(RbelHostnameFormatException.class).isThrownBy(() -> {
            RbelHostname.fromString("test__:111");
        }).withMessage("The given hostname is invalid. Please check your configuration.");
    }
}

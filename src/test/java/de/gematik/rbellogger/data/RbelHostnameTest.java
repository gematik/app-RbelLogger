package de.gematik.rbellogger.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.gematik.rbellogger.exceptions.RbelConversionException;
import org.junit.jupiter.api.Test;

public class RbelHostnameTest {
    @Test
    public void generateWithHttp_expectPort80() {
        assertThat(RbelHostname.generateFromUrl("http://foo"))
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 80);
    }

    @Test
    public void generateWithHttps_expectPort443() {
        assertThat(RbelHostname.generateFromUrl("https://foo"))
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 443);
    }

    @Test
    public void generateWithGivenPort_expectPortToBeReturned() {
        assertThat(RbelHostname.generateFromUrl("https://www.google.de:666"))
            .hasFieldOrPropertyWithValue("hostname", "www.google.de")
            .hasFieldOrPropertyWithValue("port", 666);
    }

    @Test
    public void generateWithGivenPortAndLocalUrl_expectPortToBeReturned() {
        assertThat(RbelHostname.generateFromUrl("https://foo:666"))
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 666);
    }

    @Test
    public void generateWithNoHostname_shouldFailWithException() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl(null);
        }).withMessage("Could not parse Hostname, because there is none. Is there a configuration error?");
    }

    @Test
    public void generateWithEmptyHostname_shouldFailWithException() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("");
        }).withMessage("Could not parse Hostname, because there is none. Is there a configuration error?");
    }

    @Test
    public void generateWithWrongScheme_shoudlFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("httbs://foo:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithMissingScheme_shoudlFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("foo:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithWrongHost_shoudlFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("httbs://foo__:666");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }

    @Test
    public void generateWithNegativePort_shoudlFail() {
        assertThatExceptionOfType(RbelConversionException.class).isThrownBy(() -> {
            RbelHostname.generateFromUrl("httbs://foo:-1");
        }).withMessage("The given URL is invalid. Please check your configuration.");
    }
}

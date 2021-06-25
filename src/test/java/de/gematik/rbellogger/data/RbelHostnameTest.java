package de.gematik.rbellogger.data;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(RbelHostname.generateFromUrl("https://foo:666"))
            .hasFieldOrPropertyWithValue("hostname", "foo")
            .hasFieldOrPropertyWithValue("port", 666);
    }
}
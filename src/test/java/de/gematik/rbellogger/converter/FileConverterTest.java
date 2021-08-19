/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.captures.RbelFileReaderCapturer;
import de.gematik.rbellogger.configuration.RbelFileSaveInfo;
import de.gematik.rbellogger.configuration.RbelConfiguration;
import de.gematik.rbellogger.data.RbelTcpIpMessageFacet;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

public class FileConverterTest {

    private final String filename = "target/testFileOut.rbl";

    @Test
    @SneakyThrows
    public void shouldCreateFile() {
        readPcapAndWriteFile(RbelFileSaveInfo.builder()
            .writeToFile(true)
            .clearFileOnBoot(true)
            .filename(filename)
            .build());

        assertThat(new File(filename))
            .exists();
    }

    @Test
    @SneakyThrows
    public void readFileAfterCreation_shouldContainAllMessages() {
        final RbelLogger initialRbelLogger = readPcapAndWriteFile(RbelFileSaveInfo.builder()
            .writeToFile(true)
            .clearFileOnBoot(true)
            .filename(filename)
            .build());
        initialRbelLogger.getRbelCapturer().initialize();

        final RbelLogger rbelLogger = readRbelFile();
        rbelLogger.getRbelCapturer().initialize();

        assertThat(rbelLogger.getMessageHistory())
            .hasSameSizeAs(initialRbelLogger.getMessageHistory());

        assertThat(rbelLogger.getMessageHistory().get(0)
            .getFacetOrFail(RbelTcpIpMessageFacet.class).getReceiverHostname())
            .isEqualTo(initialRbelLogger.getMessageHistory().get(0)
                .getFacetOrFail(RbelTcpIpMessageFacet.class).getReceiverHostname());
        assertThat(rbelLogger.getMessageHistory().get(0)
            .getFacetOrFail(RbelTcpIpMessageFacet.class).getSenderHostname())
            .isEqualTo(initialRbelLogger.getMessageHistory().get(0)
                .getFacetOrFail(RbelTcpIpMessageFacet.class).getSenderHostname());
        assertThat(rbelLogger.getMessageHistory().get(0)
            .getFacetOrFail(RbelTcpIpMessageFacet.class).getSequenceNumber())
            .isEqualTo(initialRbelLogger.getMessageHistory().get(0)
                .getFacetOrFail(RbelTcpIpMessageFacet.class).getSequenceNumber());
    }

    private RbelLogger readPcapAndWriteFile(RbelFileSaveInfo fileSaveInfo) throws Exception {
        final RbelLogger rbelLogger = new RbelConfiguration()
            .addCapturer(PCapCapture.builder()
                .pcapFile("src/test/resources/ssoTokenFlow.pcap")
                .printMessageToSystemOut(false)
                .build())
            .withFileSaveInfo(fileSaveInfo)
            .constructRbelLogger();
        rbelLogger.getRbelCapturer().close();

        return rbelLogger;
    }

    private RbelLogger readRbelFile() throws Exception {
        final RbelLogger rbelLogger = new RbelConfiguration()
            .addCapturer(RbelFileReaderCapturer.builder()
                .rbelFile(filename)
                .build())
            .constructRbelLogger();
        rbelLogger.getRbelCapturer().close();
        return rbelLogger;
    }
}

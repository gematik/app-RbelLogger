/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.apps;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.sun.jna.Platform;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelJsonElement;
import de.gematik.rbellogger.data.RbelJweElement;
import de.gematik.rbellogger.data.RbelMapElement;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.crypto.spec.SecretKeySpec;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapHandle.TimestampPrecision;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import wiremock.org.apache.commons.codec.digest.DigestUtils;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class PCapCapture {

    @Parameter(names = {"-device"})
    private String deviceName;

    @Parameter(names = {"-list-devices"})
    private boolean listDevices;

    @Parameter(names = {"-pcap"})
    private String pcapFile;

    @Parameter(names = {"-filter"})
    private String filter;

    @Parameter(names = {"-dump"})
    private boolean printMessageToSystemOut;

    @Parameter(names = {"-html"})
    private boolean bHtml;

    @Parameter(names = {"-shade-values"})
    private String shadeValues = null;

    private RbelConverter rbel;

    private static final BiConsumer<RbelElement, RbelConverter> RBEL_IDP_TOKEN_KEY_LISTENER = (element, converter) ->
        Optional.ofNullable(((RbelJweElement) element).getBody())
            .filter(RbelJsonElement.class::isInstance)
            .map(RbelJsonElement.class::cast)
            .map(RbelJsonElement::getJsonElement)
            .filter(RbelMapElement.class::isInstance)
            .map(RbelMapElement.class::cast)
            .map(RbelMapElement::getChildElements)
            .filter(map -> map.containsKey("token_key"))
            .map(map -> map.get("token_key"))
            .map(tokenB64 -> Base64.getUrlDecoder().decode(tokenB64.getContent()))
            .map(tokenKeyBytes -> new SecretKeySpec(tokenKeyBytes, "AES"))
            .ifPresent(aesKey -> converter.getKeyIdToKeyDatabase().put("token_key", aesKey));

    public static void main(final String[] args) {
        setWindowsNpcapPath();
        final PCapCapture main = new PCapCapture();
        JCommander.newBuilder()
            .addObject(main)
            .build()
            .parse(args);
        main.run();
    }

    private static void setWindowsNpcapPath() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            String prop = System.getProperty("jna.library.path");
            if (prop == null || prop.isEmpty()) {
                prop = "C:/Windows/System32/Npcap";
            } else {
                prop += ";C:/Windows/System32/Npcap";
            }
            System.setProperty("jna.library.path", prop);
        }
    }

    @SneakyThrows
    public void run() {
        final PcapHandle handle;
        PcapDumper dumper = null;
        if (printMessageToSystemOut) {
            log.info("Activated system out channel");
        }
        if (listDevices) {
            printAllPcapDevicesToSystemOut();
            return;
        } else if (deviceName != null) {
            handle = getLivePcapHandle();
            dumper = handle.dumpOpen("out.pcap");
        } else if (pcapFile != null) {
            handle = getOfflinePcapHandle();
        } else {
            throw new IllegalArgumentException("Either device or pcap file must be specified");
        }

        if (rbel == null) {
            rbel = RbelConverter.build(new RbelConfiguration()
                .addKey("IDP symmetricEncryptionKey",
                    new SecretKeySpec(DigestUtils.sha256("geheimerSchluesselDerNochGehashtWird"), "AES"))
                .addInitializer(new RbelKeyFolderInitializer("src/test/resources"))
                .addPostConversionListener(RbelJweElement.class, RBEL_IDP_TOKEN_KEY_LISTENER));

        }
        if (!handle.isOpen()) {
            throw new IllegalAccessException("Source not open for reading!");
        }

        if (filter == null) {
            filter = "host 127.0.0.1 and tcp port 8080";
        }
        log.info("Applying filter '" + filter + "'");
        try {
            handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
            final int maxPackets = -1;
            handle.loop(maxPackets, new RBelPacketListener(handle, dumper));
        } catch (final InterruptedException e) {
            log.info("Packet capturing interrupted...");
        } finally {
            printStatsAndCloseConnections(handle, dumper);
        }
        if (dumper != null) {
            log.info("Saved traffic to " + new File("out.pcap").getAbsolutePath());
        }

        if (bHtml) {
            exportToHtml();
        }
    }

    private void printStatsAndCloseConnections(final PcapHandle handle, final PcapDumper dumper)
        throws PcapNativeException, NotOpenException {
        if (deviceName != null && printMessageToSystemOut) {
            final PcapStat stats = handle.getStats();
            log.info("Packets received: " + stats.getNumPacketsReceived());
            log.info("Packets dropped: " + stats.getNumPacketsDropped());
            log.info("Packets dropped by interface: " + stats.getNumPacketsDroppedByIf());
            // Supported by WinPcap only
            if (Platform.isWindows()) {
                log.info("Packets captured: " + stats.getNumPacketsCaptured());
            }
        }
        if (dumper != null) {
            dumper.close();
        }
        handle.close();
    }

    private void exportToHtml() throws IOException {
        final String html;
        if (shadeValues != null) {
            final RbelValueShader shader = new RbelValueShader();
            shader.loadFromResource(shadeValues);
            html = RbelHtmlRenderer.render(rbel.getMessageHistory(), shader);
            shadeValues = null; // needed to avoid save action to make it final which breaks jcommander
        } else {
            html = RbelHtmlRenderer.render(rbel.getMessageHistory());
        }
        FileUtils.writeStringToFile(new File("out.html"), html, StandardCharsets.UTF_8);
        log.info("Saved HTML report to " + new File("out.html").getAbsolutePath());
    }

    private void printAllPcapDevicesToSystemOut() throws PcapNativeException {
        Pcaps.findAllDevs()
            .forEach(dev -> log.info((dev.isUp() ? "UP " : "DOWN ") + dev.getName() + " - " + dev.getDescription()));
    }

    private PcapHandle getLivePcapHandle() throws PcapNativeException {
        final PcapHandle handle;
        log.info("Capturing traffic live from device " + deviceName);
        final PcapNetworkInterface device = Pcaps.getDevByName(deviceName);
        final int snapshotLength = 65536; // in bytes
        final int readTimeout = 50; // in milliseconds
        handle = device.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
        return handle;
    }

    private PcapHandle getOfflinePcapHandle() throws PcapNativeException {
        PcapHandle handle;
        log.info("Reading traffic from pcap file " + new File(pcapFile).getAbsolutePath());
        try {
            handle = Pcaps.openOffline(pcapFile, TimestampPrecision.NANO);
        } catch (final PcapNativeException e) {
            handle = Pcaps.openOffline(pcapFile);
        }
        return handle;
    }

    @RequiredArgsConstructor
    class RBelPacketListener implements PacketListener {

        private final PcapHandle handle;
        private final PcapDumper dumper;
        // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding
        private boolean chunkedTransfer = false;
        private String chunkedMessage;

        @SneakyThrows
        @Override
        public void gotPacket(final Packet packet) {
            final String content = getPayloadAsString(packet);
            processMessage(content);

            try {
                if (dumper != null) {
                    dumper.dump(packet, handle.getTimestamp());
                }
            } catch (final NotOpenException e) {
                e.printStackTrace();
            }
        }

        private void processMessage(final String content) {
            try {
                if (chunkedTransfer) {
                    processChunkedMessage(content);
                }
                if (!chunkedTransfer) {
                    processSimpleHttpPackets(content);
                }
            } catch (final Exception e) {
                throw new PCapException("Failed parsing content '" + content + "'", e);
            }
        }

        private void processChunkedMessage(final String content) {
            if (!isHttp(content)) {
                final int eol = content.indexOf("\r\n"); // skip first line with length of chunked block
                chunkedMessage += eol == -1 ? content : content.substring(eol + 4);
            } else {
                final RbelElement convertedMultiPartMsg = rbel.convertMessage(chunkedMessage);
                if (printMessageToSystemOut) {
                    log.info("Multi:" + convertedMultiPartMsg.getContent());
                }
                chunkedMessage = null;
                chunkedTransfer = false;
            }
        }

        @SneakyThrows
        private void processSimpleHttpPackets(final String content) {
            if (content.contains("GET /EXIT_RBELLOGGER")) {
                handle.breakLoop();
                return;
            }
            final RbelElement convertMessage;
            if (isHttpResponse(content)) {
                convertMessage = rbel.convertMessage(content);
            } else if (isGetOrDeleteRequest(content)) {
                convertMessage = rbel.convertMessage(content);
            } else if (content.startsWith("POST ") || content.startsWith("PUT")) {
                chunkedMessage = content;
                chunkedTransfer = true;
                convertMessage = null;
            } else {
                convertMessage = rbel.convertMessage(content);
            }
            if (printMessageToSystemOut && convertMessage != null && !content.isEmpty()) {
                log.info("RBEL: " + convertMessage.getContent());
            }
        }

        private String getPayloadAsString(final Packet packet) {
            Packet pp = packet.getPayload();
            if (pp == null) {
                throw new PCapException("Payload of packet is NULL!");
            }
            int offset = 0;
            while (pp != null) {
                if (pp.getHeader() != null) {
                    offset += pp.getHeader().length();
                }
                pp = pp.getPayload();
            }
            byte[] by = packet.getPayload().getRawData();
            by = Arrays.copyOfRange(by, offset, by.length);

            return new String(by);
        }

        private boolean isHttp(final String content) {
            return isHttpRequest(content) || isHttpResponse(content);
        }

        private boolean isHttpResponse(final String content) {
            return content.startsWith("HTTP/");
        }

        private boolean isHttpRequest(final String content) {
            return isGetOrDeleteRequest(content) || isPostOrPutRequest(content);
        }

        private boolean isGetOrDeleteRequest(final String content) {
            return content.startsWith("GET ") || content.startsWith("DELETE ");
        }

        private boolean isPostOrPutRequest(final String content) {
            return content.startsWith("POST ") || content.startsWith("PUT ");
        }
    }

}

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

package de.gematik.rbellogger.captures;

import com.sun.jna.Platform;
import de.gematik.rbellogger.apps.PCapException;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelElement;
import java.io.File;
import java.util.Arrays;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapHandle.TimestampPrecision;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;

@Slf4j
public class PCapCapture extends RbelCapturer {

    private String deviceName;
    private String pcapFile;
    private String filter;
    private boolean printMessageToSystemOut;
    private Thread captureThread;
    private PcapHandle handle;
    private PcapDumper dumper;

    @Builder
    public PCapCapture(RbelConverter rbelConverter, String deviceName, String pcapFile, String filter,
        boolean printMessageToSystemOut) {
        super(rbelConverter);
        this.deviceName = deviceName;
        this.pcapFile = pcapFile;
        this.filter = filter;
        this.printMessageToSystemOut = printMessageToSystemOut;
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

    @Override
    public RbelCapturer initialize() {
        setWindowsNpcapPath();

        if (pcapFile != null) {
            getOfflinePcapHandle();
        } else if (deviceName != null) {
            getOnlineHandle();
        } else {
            throw new IllegalArgumentException("Either device or pcap file must be specified");
        }

        if (!handle.isOpen()) {
            throw new RuntimeException("Source not open for reading!");
        }

        if (filter == null) {
            filter = "host 127.0.0.1 and tcp port 8080";
        }
        log.info("Applying filter '" + filter + "'");

        captureThread = new Thread(() -> {
            try {
                handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
                final int maxPackets = -1;
                handle.loop(maxPackets, new RBelPacketListener(handle, dumper));
            } catch (final InterruptedException e) {
                log.info("Packet capturing interrupted...");
            } catch (PcapNativeException | NotOpenException e) {
                throw new RuntimeException(e);
            }
        });
        captureThread.start();

        return this;
    }

    private void getOnlineHandle() {
        try {
            getLivePcapHandle();
            dumper = handle.dumpOpen("out.pcap");
        } catch (PcapNativeException | NotOpenException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            captureThread.join();
        } catch (InterruptedException e) {
            // swallow
        }

        try {
            handle.breakLoop();
        } catch (NotOpenException e) {
            // swallow
        }

        tryToPrintStats();

        handle.close();
        if (dumper != null) {
            dumper.close();
        }
    }

    private void tryToPrintStats() {
        if (deviceName != null && printMessageToSystemOut) {
            try {
                final PcapStat stats = handle.getStats();
                log.info("Packets received: " + stats.getNumPacketsReceived());
                log.info("Packets dropped: " + stats.getNumPacketsDropped());
                log.info("Packets dropped by interface: " + stats.getNumPacketsDroppedByIf());
                // Supported by WinPcap only
                if (Platform.isWindows()) {
                    log.info("Packets captured: " + stats.getNumPacketsCaptured());
                }
            } catch (Exception e) {
                // swallow
            }
        }
    }

    @SneakyThrows
    private void getLivePcapHandle() {
        log.info("Capturing traffic live from device " + deviceName);
        final PcapNetworkInterface device = Pcaps.getDevByName(deviceName);
        final int snapshotLength = 65536; // in bytes
        final int readTimeout = 50; // in milliseconds
        handle = device.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
    }

    private void getOfflinePcapHandle() {
        log.info("Reading traffic from pcap file " + new File(pcapFile).getAbsolutePath());
        try {
            handle = Pcaps.openOffline(pcapFile, TimestampPrecision.NANO);
        } catch (final PcapNativeException e) {
            try {
                handle = Pcaps.openOffline(pcapFile);
            } catch (final PcapNativeException e1) {
                throw new RuntimeException(e1);
            }
        }
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
                final RbelElement convertedMultiPartMsg = getRbelConverter().convertMessage(chunkedMessage);
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
                convertMessage = getRbelConverter().convertMessage(content);
            } else if (isGetOrDeleteRequest(content)) {
                convertMessage = getRbelConverter().convertMessage(content);
            } else if (content.startsWith("POST ") || content.startsWith("PUT")) {
                chunkedMessage = content;
                chunkedTransfer = true;
                convertMessage = null;
            } else {
                convertMessage = getRbelConverter().convertMessage(content);
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

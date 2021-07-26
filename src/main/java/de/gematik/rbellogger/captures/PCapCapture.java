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

package de.gematik.rbellogger.captures;

import com.sun.jna.Platform;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.util.RbelException;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapHandle.TimestampPrecision;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;

@Slf4j
public class PCapCapture extends RbelCapturer {

    private String deviceName;
    private String pcapFile;
    private String filter;
    private boolean printMessageToSystemOut;
    private Thread captureThread;
    private PcapHandle handle;
    private PcapDumper dumper;
    private List<TcpPacket> unhandledTcpRequests = new ArrayList<>();
    private List<TcpPacket> unhandledTcpResponses = new ArrayList<>();
    private int tcpServerPort = -1;
    private int packetReceived = 0;
    private int tcpPacketReceived = 0;

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

        final RBelPacketListener packetListener = new RBelPacketListener(handle, dumper);

        if (pcapFile != null) {
            while (handle.isOpen()) {
                try {
                    packetListener.gotPacket(handle.getNextPacketEx());
                    log.trace(
                        "Read-In loop. Currently there are {} request and {} response TCP-Packets in their respective buffers.",
                        unhandledTcpRequests.size(), unhandledTcpResponses.size());
                } catch (EOFException e) {
                    log.info("Reached EOF");
                    break;
                } catch (TimeoutException | PcapNativeException | NotOpenException e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("After loop");
        } else {
            captureThread = new Thread(() -> {
                try {
                    handle.setFilter(filter, BpfCompileMode.OPTIMIZE);

                    final int maxPackets = -1;
                    handle.loop(maxPackets, packetListener);
                } catch (final InterruptedException e) {
                    log.info("Packet capturing interrupted...");
                    Thread.currentThread().interrupt();
                } catch (PcapNativeException | NotOpenException e) {
                    throw new RuntimeException(e);
                }
            });
            captureThread.start();
        }

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
        if (pcapFile != null) {
            initialize();
            return;
        }
        try {
            captureThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    private byte[] getCurrentBuffer(List<TcpPacket> requestList) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        requestList.stream()
            .sorted(Comparator.comparing(tcpPacket -> tcpPacket.getHeader().getSequenceNumber()))
            .map(Packet::getPayload)
            .map(Packet::getRawData)
            .forEach(data -> {
                try {
                    outputStream.write(data);
                } catch (IOException e) {
                }
            });

        return outputStream.toByteArray();
    }


    @RequiredArgsConstructor
    class RBelPacketListener implements PacketListener {

        private static final String CONTENT_LENGTH_HEADER_START = "Content-Length: ";
        private final PcapHandle handle;
        private final PcapDumper dumper;

        @SneakyThrows
        @Override
        public void gotPacket(final Packet packet) {
            Optional<TcpPacket> tcpPacket = extractTcpPacket(packet);
            if (tcpServerPort == -1
                && tcpPacket.isPresent()
                && tcpPacket.get().getHeader().getSyn()
                && !tcpPacket.get().getHeader().getAck()) {
                tcpServerPort = tcpPacket.get().getHeader().getDstPort().valueAsInt();
            }

            packetReceived++;
            if (tcpPacket.isEmpty() ||
                tcpPacket.get().getPayload() == null) {
                return;
            }
            tcpPacketReceived++;

            final Pair<RbelHostname, RbelHostname> ipAddresses = getIpAddresses(packet);
            if (tcpPacket.get().getHeader().getDstPort().valueAsInt() == tcpServerPort) {
                addToBufferAndExtractCompletedMessages(tcpPacket, unhandledTcpRequests,
                    ipAddresses.getKey(), ipAddresses.getValue());
            } else {
                addToBufferAndExtractCompletedMessages(tcpPacket, unhandledTcpResponses,
                    ipAddresses.getKey(), ipAddresses.getValue());
            }

            if ((tcpPacketReceived % 1_000) == 0) {
                log.info("Received {} TCP-Packets from {} packets overall", tcpPacketReceived, packetReceived);
            }
            try {
                if (dumper != null) {
                    dumper.dump(packet, handle.getTimestamp());
                }
            } catch (final NotOpenException e) {
                throw new RuntimeException("Encountered exception while receiving", e);
            }
        }

        private Pair<RbelHostname, RbelHostname> getIpAddresses(Packet packet) {
            Optional<Integer> srcPort = Optional.empty();
            Optional<String> srcIpAddress = Optional.empty();
            Optional<Integer> dstPort = Optional.empty();
            Optional<String> dstIpAddress = Optional.empty();
            do {
                if (packet instanceof IpV4Packet) {
                    dstIpAddress = Optional.of(((IpV4Packet) packet)
                        .getHeader().getDstAddr().getHostAddress());
                    srcIpAddress = Optional.of(((IpV4Packet) packet)
                        .getHeader().getSrcAddr().getHostAddress());
                }
                if (packet instanceof TcpPacket) {
                    dstPort = Optional.of(((TcpPacket) packet)
                        .getHeader().getDstPort().valueAsInt());
                    srcPort = Optional.of(((TcpPacket) packet)
                        .getHeader().getSrcPort().valueAsInt());
                }
                packet = packet.getPayload();
            } while (packet.getPayload() != null);
            if (srcPort.isEmpty() || srcIpAddress.isEmpty()
                || dstPort.isEmpty() || dstIpAddress.isEmpty()) {
                throw new RbelException("Error while trying to gather src/dst Data from " + packet);
            }
            return Pair.of(
                new RbelHostname(srcIpAddress.get(), srcPort.get()),
                new RbelHostname(dstIpAddress.get(), dstPort.get())
            );
        }

        private void addToBufferAndExtractCompletedMessages(Optional<TcpPacket> tcpPacket, List<TcpPacket> buffer,
            RbelHostname sender, RbelHostname recipient) {
            tcpPacket.ifPresent(buffer::add);
            Optional<byte[]> nextMessage = extractCompleteHttpMessage(getCurrentBuffer(buffer));
            if (nextMessage.isPresent()) {
                processSimpleHttpPackets(nextMessage.get(), sender, recipient);
                buffer.clear();
            }
        }

        private Optional<byte[]> extractCompleteHttpMessage(byte[] currentBuffer) {
            String dumpString = new String(currentBuffer, StandardCharsets.US_ASCII);
            if (!isHttp(dumpString)) {
                log.trace("No HTTP-message recognized, skipping");
                return Optional.empty();
            }
            String[] messageParts = dumpString.split("\r\n\r\n");
            String[] headerFields = messageParts[0].split("\r\n");
            Optional<Integer> messageLength = Stream.of(headerFields)
                .filter(field -> field.startsWith(CONTENT_LENGTH_HEADER_START))
                .map(field -> field.substring(CONTENT_LENGTH_HEADER_START.length()))
                .filter(NumberUtils::isParsable)
                .map(Integer::parseInt)
                .findAny();
            if (messageLength.isPresent()) {
                if (messageParts.length < 2) {
                    if (messageLength.isEmpty() || messageLength.get() == 0) {
                        return Optional.of(currentBuffer);
                    } else {
                        log.trace("Header found, body segmented away. \n'{}'", dumpString);
                        return Optional.empty();
                    }
                } else if (messageParts[1].length() == messageLength.get()
                || messageParts[1].length() == messageLength.get() + 1) {
                    return Optional.of(currentBuffer);
                } else if (messageParts[1].length() > messageLength.get()) {
                    throw new RuntimeException(
                        "Overshot while parsing message (collected more bytes then the message has)");
                } else {
                    log.trace("Message not yet complete. Wanted {} bytes, but found only {}", messageLength.get(),
                        messageParts[1].length());
                    return Optional.empty();
                }
            } else {
                boolean chunked = Arrays.asList(headerFields).contains("Transfer-Encoding: chunked");
                if (!chunked) {
                    log.trace("Returning (hopefully) body-less message");
                    return Optional.of(currentBuffer);
                }
                if (!dumpString.endsWith("0\r\n\r\n")) {
                    log.trace("Chunked message, incomplete");
                    return Optional.empty();
                }
                log.trace("Returning chunked message");
                return Optional.ofNullable(currentBuffer);
            }
        }

        private Optional<TcpPacket> extractTcpPacket(Packet packet) {
            Packet ptr = packet;
            while ((ptr != null) && !(ptr instanceof TcpPacket)) {
                ptr = ptr.getPayload();
            }
            if (ptr instanceof TcpPacket) {
                return Optional.ofNullable((TcpPacket) ptr);
            } else {
                return Optional.empty();
            }
        }

        @SneakyThrows
        private void processSimpleHttpPackets(final byte[] content, RbelHostname sender, RbelHostname recipient) {
            final RbelElement convertedMessage = getRbelConverter().parseMessage(content, sender, recipient);
            if (printMessageToSystemOut && convertedMessage != null && content.length > 0) {
                if (convertedMessage.getRawStringContent() != null) {
                    log.trace("RBEL: " + convertedMessage.getRawStringContent());
                } else {
                    log.trace("RBEL: <null> message encountered!");
                }
            }
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

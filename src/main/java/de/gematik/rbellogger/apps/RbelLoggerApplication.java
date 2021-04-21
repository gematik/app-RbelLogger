package de.gematik.rbellogger.apps;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
import de.gematik.rbellogger.captures.RbelCapturer;
import de.gematik.rbellogger.captures.WiremockCapture;
import de.gematik.rbellogger.converter.RbelConfiguration;
import de.gematik.rbellogger.converter.initializers.RbelKeyFolderInitializer;
import de.gematik.rbellogger.data.RbelJweElement;
import de.gematik.rbellogger.key.RbelKey;
import de.gematik.rbellogger.key.RbelKeyManager;
import de.gematik.rbellogger.renderer.RbelHtmlRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import wiremock.org.apache.commons.codec.digest.DigestUtils;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RbelLoggerApplication {

    @Parameter(names = {"-device", "-d"},
        description = "Listen on PCAP-Device. (Starts the application in PCAP-mode)")
    private String deviceName;
    @Parameter(names = {"-keyFolder"},
        description = "A folder to be scanned for key-material. Default is '.'. "
            + "P12-Files in this folder are collected and will be used to decipher object or check signatures.")
    private String keyFolder = ".";
    @Parameter(names = {"-list-devices"},
        description = "List all available PCAP-devices.")
    private boolean listDevices;
    @Parameter(names = {"-pcap"},
        description = "Read the data from the following pcap-file. (Starts the application in offline-PCAP-mode)")
    private String pcapFile;
    @Parameter(names = {"-filter"},
        description = "Define a filter for a PCAP-capture")
    private String filter;
    @Parameter(names = {"--proxyFor", "-p"},
        description = "Start a proxy server for the given url. (Starts the application in Wiremock-Mode)")
    private String proxyFor;
    @Parameter(names = {"-dump"},
        description = "Should the captured messages be print to standard-out immediately?")
    private boolean printMessageToSystemOut;
    @Parameter(names = {"-html"},
        description = "Write the captured traffic to the following file. Default is 'out.html'.")
    @Builder.Default
    private String htmlFile = "out.html";
    @Parameter(names = {"-h", "--help", "-?"},
        description = "Show help")
    @Builder.Default
    private boolean help = false;

    public static void main(String[] args) {
        setWindowsNpcapPath();
        final RbelLoggerApplication main = new RbelLoggerApplication();
        final JCommander jCommander = JCommander.newBuilder()
            .addObject(main)
            .build();
        jCommander.parse(args);
        if (main.help) {
            jCommander.usage();
            return;
        }
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
        if (printMessageToSystemOut) {
            log.info("Activated system out channel");
        }
        if (listDevices) {
            printAllPcapDevicesToSystemOut();
            return;
        } else {
            final RbelLogger rbelLogger = RbelLogger.build(new RbelConfiguration()
                .addKey("IDP symmetricEncryptionKey",
                    new SecretKeySpec(DigestUtils.sha256("geheimerSchluesselDerNochGehashtWird"), "AES"),
                    RbelKey.PRECEDENCE_KEY_FOLDER)
                .addInitializer(new RbelKeyFolderInitializer(keyFolder))
                .addPostConversionListener(RbelJweElement.class, RbelKeyManager.RBEL_IDP_TOKEN_KEY_LISTENER)
                .addCapturer(getCapturer())
            );

            if (StringUtils.isNotBlank(htmlFile)) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        FileUtils.writeStringToFile(new File(htmlFile),
                            RbelHtmlRenderer
                                .render(rbelLogger.getMessageHistory()), Charset.defaultCharset());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    log.info("Saved HTML report to " + new File(htmlFile).getAbsolutePath());
                }));
            }

            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private RbelCapturer getCapturer() {
        if (StringUtils.isNotBlank(deviceName)) {
            return PCapCapture.builder()
                .printMessageToSystemOut(true)
                .deviceName(deviceName)
                .filter(filter)
                .build();
        } else if (StringUtils.isNotBlank(proxyFor)) {
            return WiremockCapture.builder()
                .proxyFor(proxyFor)
                .build();
        } else {
            throw new IllegalStateException("Either deviceName or proxyFor has to be set!");
        }
    }

    private void printAllPcapDevicesToSystemOut() throws PcapNativeException {
        Pcaps.findAllDevs()
            .forEach(dev -> log.info((dev.isUp() ? "UP " : "DOWN ") + dev.getName() + " - " + dev.getDescription()));
    }
}

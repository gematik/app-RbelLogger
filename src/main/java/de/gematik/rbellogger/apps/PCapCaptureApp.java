package de.gematik.rbellogger.apps;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.captures.PCapCapture;
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
public class PCapCaptureApp {

    @Parameter(names = {"-device"})
    @Builder.Default
    private String deviceName = "lo0";
    @Parameter(names = {"-keyFolder"})
    @Builder.Default
    private String keyFolder = "src/test/resources";
    @Parameter(names = {"-list-devices"})
    private boolean listDevices;
    @Parameter(names = {"-pcap"})
    private String pcapFile;
    @Parameter(names = {"-filter"})
    private String filter;
    @Parameter(names = {"-dump"})
    private boolean printMessageToSystemOut;
    @Parameter(names = {"-html"})
    @Builder.Default
    private String htmlFile = "out.html";

    public static void main(final String[] args) {
        setWindowsNpcapPath();
        final PCapCaptureApp main = new PCapCaptureApp();
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
                .addCapturer(
                    PCapCapture.builder()
                        .printMessageToSystemOut(true)
                        .deviceName(deviceName)
                        .filter(filter)
                        .build())
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

    private void printAllPcapDevicesToSystemOut() throws PcapNativeException {
        Pcaps.findAllDevs()
            .forEach(dev -> log.info((dev.isUp() ? "UP " : "DOWN ") + dev.getName() + " - " + dev.getDescription()));
    }
}

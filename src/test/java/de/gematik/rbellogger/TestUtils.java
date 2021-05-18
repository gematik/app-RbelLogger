package de.gematik.rbellogger;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class TestUtils {
    public static String readCurlFromFileWithCorrectedLineBreaks(String fileNmae) throws IOException {
        return FileUtils
            .readFileToString(new File(fileNmae))
            .replace("\r\n", "\r\n")
            .replace("\n", "\r\n");
    }
}

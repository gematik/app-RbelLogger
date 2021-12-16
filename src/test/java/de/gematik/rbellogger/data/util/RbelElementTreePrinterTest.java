package de.gematik.rbellogger.data.util;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.RbelOptions;
import de.gematik.rbellogger.data.RbelElement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Function;

import static de.gematik.rbellogger.TestUtils.readCurlFromFileWithCorrectedLineBreaks;

public class RbelElementTreePrinterTest {

    @Test
    public void printFacets() throws IOException {
        RbelOptions.activateFacetsPrinting();

        System.out.println(readAndConvertCurlMessage("src/test/resources/sampleMessages/xmlMessage.curl").printTreeStructure());
    }

    private static RbelElement readAndConvertCurlMessage(String fileName, Function<String, String>... messageMappers) throws IOException {
        String curlMessage = readCurlFromFileWithCorrectedLineBreaks(fileName);
        for (Function<String, String> mapper : messageMappers) {
            curlMessage = mapper.apply(curlMessage);
        }
        return RbelLogger.build().getRbelConverter().convertElement(curlMessage, null);
    }
}
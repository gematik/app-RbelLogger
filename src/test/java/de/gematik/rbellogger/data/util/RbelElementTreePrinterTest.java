/*
 * Copyright (c) 2022 gematik GmbH
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

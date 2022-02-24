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

package de.gematik.rbellogger;

import de.gematik.rbellogger.data.RbelHostname;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class TestUtils {

    public static String readCurlFromFileWithCorrectedLineBreaks(String fileName) throws IOException {
        return readCurlFromFileWithCorrectedLineBreaks(fileName, Charset.defaultCharset());
    }

    public static String readCurlFromFileWithCorrectedLineBreaks(String fileName, Charset charset) throws IOException {
        return FileUtils.readFileToString(new File(fileName), charset)
            .replaceAll("(?<!\\r)\\n", "\r\n");
    }

    public static RbelHostname localhostWithPort(int tcpPort) {
        return RbelHostname.builder()
            .hostname("localhost")
            .port(tcpPort)
            .build();
    }
}

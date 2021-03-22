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

package de.gematik.rbellogger.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelValueShader;
import de.gematik.rbellogger.data.RbelElement;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class RbelHtmlRendererTest {

    @Test
    public void convertToHtml() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelElement convertedMessage = RbelConverter.build().convertMessage(curlMessage);

        FileUtils.writeStringToFile(new File("target/out.html"),
            RbelHtmlRenderer.render(List.of(convertedMessage)), Charset.defaultCharset());
    }

    @Test
    public void valueShading() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelElement convertedMessage = RbelConverter.build().convertMessage(curlMessage);

        final String convertedHtml = RbelHtmlRenderer.render(List.of(convertedMessage), new RbelValueShader(
            Map.of("Date", "<halt ein date>",
                "Content-Length", "<Die Länge. Hier %s>",
                "exp", "<Nested Shading>")
        ));

        assertThat(convertedHtml)
            .doesNotContain("Fri, 26 Feb 2021")
            .contains("&lt;Die Länge. Hier 2653&gt;")

            .doesNotContain("1614425703")
            .contains("\\u003cNested Shading\\u003e");

        FileUtils.writeStringToFile(new File("target/out.html"), convertedHtml);
    }
}

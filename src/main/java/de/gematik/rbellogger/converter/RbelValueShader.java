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

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.RbelElement;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.SneakyThrows;

public class RbelValueShader {

    private final Map<String, String> formatStrings = new HashMap<>();

    public RbelValueShader() {
    }

    @SneakyThrows
    public void loadFromResource(final String fileOrResource) {
        final Properties p = new Properties();
        if (fileOrResource.startsWith("classpath:")) {
            try (final InputStream is = getClass()
                .getResourceAsStream(fileOrResource.substring("classpath:".length()))) {
                p.load(is);
            }
        } else {
            try (final FileInputStream fis = new FileInputStream(fileOrResource)) {
                p.load(fis);
            }
        }
        p.forEach((key, value) -> formatStrings.put(key.toString(), value.toString()));
    }

    public RbelValueShader(final Map<String, String> formatStrings) {
        this.formatStrings.putAll(formatStrings);
    }

    public boolean shouldConvert(String attributeName) {
        return formatStrings.containsKey(attributeName);
    }

    public String convert(final String attributeName, final Object value) {
        if (formatStrings.containsKey(attributeName)) {
            return String.format(formatStrings.get(attributeName), toStringValue(value));
        } else {
            return toStringValue(value);
        }
    }

    private String toStringValue(final Object value) {
        if (value instanceof RbelElement) {
            return ((RbelElement) value).getContent();
        } else {
            return value.toString();
        }
    }

    // TODO do we need to differ attribute for different RbelElements (JSON, JWT, JWE, Header/Bodymaps)?
    // TODO do we need to introduce special fromatters for dates, int, float values or can we live with implicit toString() on all?
}

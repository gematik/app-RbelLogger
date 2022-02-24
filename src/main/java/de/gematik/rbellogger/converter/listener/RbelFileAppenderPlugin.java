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

package de.gematik.rbellogger.converter.listener;

import de.gematik.rbellogger.configuration.RbelFileSaveInfo;
import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.converter.RbelConverterPlugin;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelHttpMessageFacet;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static de.gematik.rbellogger.util.RbelFileWriterUtils.convertToRbelFileString;

@Data
public class RbelFileAppenderPlugin implements RbelConverterPlugin {

    private final RbelFileSaveInfo fileSaveInfo;

    public RbelFileAppenderPlugin(RbelFileSaveInfo fileSaveInfo) {
        this.fileSaveInfo = fileSaveInfo;
        if (fileSaveInfo.isWriteToFile()
            && StringUtils.isNotEmpty(fileSaveInfo.getFilename())
            && fileSaveInfo.isClearFileOnBoot()) {
            FileUtils.deleteQuietly(new File(fileSaveInfo.getFilename()));
        }
    }

    @Override
    public void consumeElement(RbelElement rbelElement, RbelConverter converter) {
        if (fileSaveInfo.isWriteToFile()
            && StringUtils.isNotEmpty(fileSaveInfo.getFilename())
            && rbelElement.hasFacet(RbelHttpMessageFacet.class)) {
            try {
                FileUtils.writeStringToFile(new File(fileSaveInfo.getFilename()),
                    convertToRbelFileString(rbelElement), Charset.defaultCharset(), true);
            } catch (IOException e) {
                throw new RuntimeException("Exception while trying to save RbelElement to file '" + fileSaveInfo.getFilename() + "'!", e);
            }
        }
    }

}

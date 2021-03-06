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
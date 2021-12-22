package de.gematik.rbellogger.captures;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.util.RbelFileWriterUtils;
import lombok.Builder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RbelFileReaderCapturer extends RbelCapturer {

    private final String rbelFile;

    @Builder
    public RbelFileReaderCapturer(RbelConverter rbelConverter, String rbelFile) {
        super(rbelConverter);
        this.rbelFile = rbelFile;
    }

    @Override
    public RbelCapturer initialize() {
        try {
            RbelFileWriterUtils.convertFromRbelFile(Files.readString(Paths.get(rbelFile)), getRbelConverter());
            return this;
        } catch (IOException | UncheckedIOException e) {
            throw new RbelFileIoException("Error while reading from rbel-file with path '" + rbelFile + "'", e);
        }
    }

    @Override
    public void close() throws Exception {
        // no open handles
    }

    private class RbelFileIoException extends RuntimeException {
        public RbelFileIoException(String s, Exception e) {
            super(s, e);
        }
    }
}

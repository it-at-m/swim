package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.io.InputStream;

public record LoadedFile(
        FileReference fileReference,
        FileReference decodedFileReference,
        InputStream content,
        Metadata metadata) {
    // TODO rm and only allow fileReference?
    public FileReference decodedFileReference() {
        if (decodedFileReference != null) {
            return decodedFileReference;
        }
        return fileReference;
    }
}

package de.muenchen.oss.swim.dms.adapter.out.dms;

import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;

public class NamedInputStreamResource extends InputStreamResource {
    private final String filename;

    public NamedInputStreamResource(final String filename, final InputStream inputStream) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long contentLength() {
        return -1;
    }
}

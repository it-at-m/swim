package de.muenchen.oss.swim.dms.adapter.out.dms;

import lombok.EqualsAndHashCode;
import org.springframework.core.io.ByteArrayResource;

@EqualsAndHashCode(callSuper = true)
public class NamedByteArrayRessource extends ByteArrayResource {
    private final String filename;

    public NamedByteArrayRessource(final String filename, final byte[] bytes) {
        super(bytes);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}

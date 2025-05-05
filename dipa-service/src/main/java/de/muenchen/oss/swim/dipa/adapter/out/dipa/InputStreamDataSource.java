package de.muenchen.oss.swim.dipa.adapter.out.dipa;

import jakarta.activation.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class InputStreamDataSource implements DataSource {
    private final InputStream inputStream;

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getContentType() {
        return "*/*";
    }

    @Override
    public String getName() {
        return "InputStreamDataSource";
    }
}

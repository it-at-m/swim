package de.muenchen.oss.swim.dms.application.port.out;

import jakarta.validation.constraints.NotBlank;
import java.io.InputStream;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileSystemOutPort {
    /**
     * Get file via presigned url.
     *
     * @param presignedUrl The presigned url for the file.
     * @return The file.
     */
    InputStream getPresignedUrlFile(@NotBlank String presignedUrl);
}

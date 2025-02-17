package de.muenchen.oss.swim.libs.handlercore.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import org.junit.jupiter.api.Test;

class FileTest {
    @Test
    void testFromPresignedUrl() throws PresignedUrlException {
        final File file = File.fromPresignedUrl("https://s3.example.com/bucket/test/deep/path/file.pdf?param1=example");
        assertEquals("bucket", file.bucket());
        assertEquals("test/deep/path/file.pdf", file.path());
        assertEquals("file.pdf", file.getFileName());
    }
}

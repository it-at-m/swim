package de.muenchen.oss.swim.dispatcher.domain.model;

import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import org.junit.jupiter.api.Test;

class FileReferenceTest {

    @Test
    void testMethods() {
        final FileReference file = new FileReference("test-bucket", "test/path/reference.pdf");
        assertEquals("test/path", file.getParentPath());
        assertEquals("path", file.getParentName());
        assertEquals("reference.pdf", file.getFileName());
        assertEquals("reference", file.getFileNameWithoutExtension());
        assertEquals("test/path/reference.json", file.getMetadataFilePath());
        assertEquals(new FileReference("test-bucket", "test/path/reference.json"), file.getMetadataFile());
    }

    @Test
    void testFromPresignedUrl() throws PresignedUrlException {
        final FileReference file = FileReference.fromPresignedUrl(TEST_PRESIGNED_URL);
        assertEquals(TEST_PRESIGNED_URL_FILE, file);
    }
}

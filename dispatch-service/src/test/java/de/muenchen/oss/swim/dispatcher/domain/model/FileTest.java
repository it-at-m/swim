package de.muenchen.oss.swim.dispatcher.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileTest {

    @Test
    void test() {
        final FileReference file = new FileReference("test-bucket", "test/path/reference.pdf");
        assertEquals("test/path", file.getParentPath());
        assertEquals("path", file.getParentName());
        assertEquals("reference.pdf", file.getFileName());
        assertEquals("reference", file.getFileNameWithoutExtension());
        assertEquals("test/path/reference.json", file.getMetadataFilePath());
        assertEquals(new FileReference("test-bucket", "test/path/reference.json"), file.getMetadataFile());
    }
}

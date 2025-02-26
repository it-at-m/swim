package de.muenchen.oss.swim.dispatcher.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileTest {

    @Test
    void test() {
        final File file = new File("test-bucket", "test/path/file.pdf", 0L);
        assertEquals("test/path", file.getParentPath());
        assertEquals("path", file.getParentName());
        assertEquals("file.pdf", file.getFileName());
        assertEquals("file", file.getFileNameWithoutExtension());
    }
}

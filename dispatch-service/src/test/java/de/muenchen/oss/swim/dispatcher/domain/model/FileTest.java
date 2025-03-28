package de.muenchen.oss.swim.dispatcher.domain.model;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileTest {

    @Test
    void test() {
        final File file = new File(TENANT, BUCKET, "test/path/file.pdf", 0L);
        assertEquals("test/path", file.getParentPath());
        assertEquals("path", file.getParentName());
        assertEquals("file.pdf", file.getFileName());
        assertEquals("file", file.getFileNameWithoutExtension());
    }
}

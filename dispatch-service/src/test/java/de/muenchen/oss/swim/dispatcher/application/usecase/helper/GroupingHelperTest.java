package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import static de.muenchen.oss.swim.dispatcher.TestConstants.createFileWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDispatcherProperties.class, GroupingHelper.class })
@EnableConfigurationProperties
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class GroupingHelperTest {
    public static final String BASE_NAME_A = "fileA";
    public static final String BASE_NAME_B = "fileB";
    public static final String BASE_NAME_OTHER = "other";

    @Autowired
    private GroupingHelper helper;

    @Test
    void groupFiles_groupsChunkedAndNonChunked() {
        // given
        final Map<String, String> tags = Map.of("k", "v");
        final FileWithMetadata a1 = createFileWithMeta("path/fileA-1v3.pdf", tags);
        final FileWithMetadata a2 = createFileWithMeta("path/fileA-2v3.pdf", tags);
        final FileWithMetadata a3 = createFileWithMeta("path/fileA-3v3.pdf", tags);
        final FileWithMetadata other = createFileWithMeta("path/other.pdf", tags);
        final FileWithMetadata b1 = createFileWithMeta("path/fileB-1v2.pdf", tags);
        final FileWithMetadata b2 = createFileWithMeta("path/fileB-2v2.pdf", tags);

        // when
        final Map<String, FileGroup> grouped = helper.groupFiles(List.of(a1, a2, a3, other, b1, b2));

        // then
        assertEquals(Set.of(BASE_NAME_A, BASE_NAME_B, BASE_NAME_OTHER), grouped.keySet());
        assertEquals(3, grouped.get(BASE_NAME_A).getFiles().size());
        assertTrue(grouped.get(BASE_NAME_A).isMulti());
        assertEquals(2, grouped.get(BASE_NAME_B).getFiles().size());
        assertTrue(grouped.get(BASE_NAME_B).isMulti());
        assertEquals(1, grouped.get(BASE_NAME_OTHER).getFiles().size());
        assertFalse(grouped.get(BASE_NAME_OTHER).isMulti());
    }
}

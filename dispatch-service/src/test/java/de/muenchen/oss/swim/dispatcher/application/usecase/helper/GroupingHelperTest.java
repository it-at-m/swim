package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import static de.muenchen.oss.swim.dispatcher.TestConstants.createFileWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
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
        final FileWithMetadata b1 = createFileWithMeta("path/fileB_1v2.pdf", tags);
        final FileWithMetadata b2 = createFileWithMeta("path/fileB_2v2.pdf", tags);

        // when
        final Map<String, List<FileWithMetadata>> grouped = helper.groupFiles(List.of(a1, a2, a3, other, b1, b2));

        // then
        assertEquals(Set.of("fileA", "fileB", "other"), grouped.keySet());
        assertEquals(3, grouped.get("fileA").size());
        assertEquals(2, grouped.get("fileB").size());
        assertEquals(1, grouped.get("other").size());
    }
}

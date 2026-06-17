package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1_BASE_NAME;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1_GROUP;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TAGS;
import static de.muenchen.oss.swim.dispatcher.TestConstants.createFileWithMeta;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileChunkException;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { SwimDispatcherProperties.class, ValidationHelper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class ValidationHelperTest {
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;
    @MockitoSpyBean
    @Autowired
    private ValidationHelper helper;

    @Test
    void validateFileGroup_Multi() throws FileSizeException, FileChunkException {
        // given
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final FileWithMetadata tf1 = createFileWithMeta("p/x-1v2.pdf", TAGS);
        final FileWithMetadata tf2 = createFileWithMeta("p/x-2v2.pdf", TAGS);
        final FileGroup fileGroup = new FileGroup(true, List.of(tf1, tf2));
        // call
        helper.validateFileGroup(useCase, "x", fileGroup);
        // then
        verify(helper).validateGroup(eq("x"), eq(List.of(tf1, tf2)));
        verify(helper).validateFile(eq(useCase), eq(tf1));
        verify(helper).validateFile(eq(useCase), eq(tf2));
    }

    @Test
    void validateFileGroup_Single() throws FileSizeException, FileChunkException {
        // given
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // call
        helper.validateFileGroup(useCase, FILE1_BASE_NAME, FILE1_GROUP);
        // then
        verify(helper).validateFile(eq(useCase), eq(FILE1));
    }

    @Test
    void validateGroup_ok_whenAllChunksPresentAndTagsEqual() {
        // given
        final Map<String, String> tags = Map.of("state", "ok");
        final List<FileWithMetadata> group = List.of(
                createFileWithMeta("p/base-1v3.pdf", tags),
                createFileWithMeta("p/base-2v3.pdf", tags),
                createFileWithMeta("p/base-3v3.pdf", tags));

        // then
        assertDoesNotThrow(() -> helper.validateGroup("base", group));
    }

    @Test
    void validateGroup_throws_whenTagsDiffer() {
        // given
        final List<FileWithMetadata> group = List.of(
                createFileWithMeta("p/x-1v2.pdf", Map.of("t", "1")),
                createFileWithMeta("p/x-2v2.pdf", Map.of("t", "2")));

        // then
        assertThrows(FileChunkException.class, () -> helper.validateGroup("x", group));
    }

    @Test
    void validateGroup_throws_whenChunkMissing() {
        // given: total declares 3, but only 1 and 3 present and max time is over
        final Map<String, String> tags = Map.of("same", "true");
        final Duration age = Duration.ofDays(2);
        final List<FileWithMetadata> group = List.of(
                createFileWithMeta("p/y-1v3.pdf", tags, age),
                createFileWithMeta("p/y-3v3.pdf", tags, age));

        // then
        assertThrows(FileChunkException.class, () -> helper.validateGroup("y", group));
    }

    @Test
    void validateGroup_false_whenChunkMissingBellowTimeout() throws FileChunkException {
        // given: total declares 3, but only 1 and 3 present
        final Map<String, String> tags = Map.of("same", "true");
        final List<FileWithMetadata> group = List.of(
                createFileWithMeta("p/y-1v3.pdf", tags),
                createFileWithMeta("p/y-3v3.pdf", tags));

        // then
        assertFalse(helper.validateGroup("y", group));
    }

    @Test
    void validateFile_throwsFileSize() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        assertThrows(FileSizeException.class,
                () -> helper.validateFile(useCase,
                        new FileWithMetadata(new FileReference(BUCKET, "test.pdf"), useCase.getMaxFileSize().toBytes() + 1, null, TAGS)));
    }
}

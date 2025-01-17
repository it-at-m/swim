package de.muenchen.swim.dispatcher.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.swim.dispatcher.TestConstants;
import de.muenchen.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.swim.dispatcher.domain.model.UseCase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { SwimDispatcherProperties.class, MarkFileFinishedUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class MarkFileFinishedUseCaseTest {
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoSpyBean
    @Autowired
    private MarkFileFinishedUseCase markFileFinishedUseCase;

    private static final String TEST_USE_CASE = "test-meta";
    private static final String TEST_PRESIGNED_URL = "https://s3.muenchen.de/test-bucket/test/path/example.pdf";

    @Test
    void testMarkFileFinished_Success() throws PresignedUrlException {
        when(fileSystemOutPort.verifyPresignedUrl(any())).thenReturn(true);
        markFileFinishedUseCase.markFileFinished(TEST_USE_CASE, TEST_PRESIGNED_URL);
        verify(fileSystemOutPort, times(1)).verifyPresignedUrl(TEST_PRESIGNED_URL);
        verify(markFileFinishedUseCase, times(1)).finishFile(eq(TEST_USE_CASE), eq(TEST_PRESIGNED_URL));
        verify(fileSystemOutPort, times(1)).tagFile(eq("test-bucket"), eq("test/path/example.pdf"), eq(Map.of(
                "SWIM_Status", "finished")));
        verify(notificationOutPort, times(0)).sendFileFinishError(any(), any(), any(), any());
    }

    @Test
    void testMarkFileFinished_PresignedUrlException() throws PresignedUrlException {
        when(fileSystemOutPort.verifyPresignedUrl(any())).thenReturn(false);
        markFileFinishedUseCase.markFileFinished(TEST_USE_CASE, TEST_PRESIGNED_URL);
        verify(notificationOutPort, times(1)).sendFileFinishError(any(), eq(TEST_USE_CASE), eq(TEST_PRESIGNED_URL), any(PresignedUrlException.class));
    }

    @Test
    void testMarkFileFinished_UseCaseException() {
        markFileFinishedUseCase.markFileFinished("unknown-usecase", TEST_PRESIGNED_URL);
        verify(notificationOutPort, times(1)).sendFileFinishError(any(), eq("unknown-usecase"), eq(TEST_PRESIGNED_URL), any(UseCaseException.class));
    }

    @Test
    void testFindUseCase() throws UseCaseException {
        // found
        final UseCase useCase = markFileFinishedUseCase.findUseCase(TEST_USE_CASE);
        assertEquals(TEST_USE_CASE, useCase.getName());
        // not found
        assertThrows(UseCaseException.class, () -> markFileFinishedUseCase.findUseCase("unknown-usecase"));
    }
}

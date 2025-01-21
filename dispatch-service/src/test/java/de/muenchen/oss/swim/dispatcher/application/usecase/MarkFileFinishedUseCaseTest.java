package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_USE_CASE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
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
    @MockitoSpyBean
    @Autowired
    private MarkFileFinishedUseCase markFileFinishedUseCase;

    @Test
    void testMarkFileFinished_Success() throws PresignedUrlException, UseCaseException {
        when(fileSystemOutPort.verifyPresignedUrl(any())).thenReturn(true);
        markFileFinishedUseCase.markFileFinished(TEST_USE_CASE, TEST_PRESIGNED_URL);
        verify(fileSystemOutPort, times(1)).verifyPresignedUrl(TEST_PRESIGNED_URL);
        verify(fileSystemOutPort, times(1)).tagFile(eq("test-bucket"), eq("test/path/example.pdf"), eq(Map.of(
                "SWIM_State", "finished")));
    }

    @Test
    void testMarkFileFinished_PresignedUrlException() throws PresignedUrlException {
        when(fileSystemOutPort.verifyPresignedUrl(any())).thenReturn(false);
        assertThrows(PresignedUrlException.class, () -> markFileFinishedUseCase.markFileFinished(TEST_USE_CASE, TEST_PRESIGNED_URL));
    }

    @Test
    void testMarkFileFinished_UseCaseException() {
        assertThrows(UseCaseException.class, () -> markFileFinishedUseCase.markFileFinished("unknown-usecase", TEST_PRESIGNED_URL));
    }
}

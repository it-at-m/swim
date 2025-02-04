package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_USE_CASE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { SwimDispatcherProperties.class, ErrorHandlerUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class ErrorHandlerUseCaseTest {
    @MockitoBean
    private DispatchMeter dispatchMeter;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @Autowired
    private ErrorHandlerUseCase errorHandlerUseCase;

    private static final ErrorDetails TEST_ERROR_DETAILS = new ErrorDetails(
            "swim-test-local",
            "de.muenchen.swim.CustomException",
            "Error message",
            "Error stacktrace");

    @Test
    void handleError_Success() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // call
        errorHandlerUseCase.handleError(TEST_USE_CASE, TEST_PRESIGNED_URL, null, TEST_ERROR_DETAILS);
        // test
        verify(notificationOutPort, times(1)).sendFileError(eq(useCase.getMailAddresses()), eq(TEST_USE_CASE), eq(TEST_PRESIGNED_URL_PATH),
                eq(TEST_ERROR_DETAILS));
        verify(fileSystemOutPort, times(1)).tagFile(any(), any(), any());
        verify(notificationOutPort, times(0)).sendFileError(any(), any(), any(), any(), any());
        verify(dispatchMeter, times(1)).incrementError(eq(TEST_USE_CASE), eq(TEST_ERROR_DETAILS.source()));
    }

    @Test
    void handleError_PresignedUrlException() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // call
        errorHandlerUseCase.handleError(TEST_USE_CASE, null, null, TEST_ERROR_DETAILS);
        // test
        verify(notificationOutPort, times(0)).sendFileError(any(), any(), any(), any());
        verify(notificationOutPort, times(0)).sendFileError(eq(useCase.getMailAddresses()), eq(TEST_USE_CASE), eq(TEST_PRESIGNED_URL_PATH),
                eq(TEST_ERROR_DETAILS), any());
    }
}

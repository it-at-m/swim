package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FOLDER_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_DISPATCH_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_RECIPIENTS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.util.List;
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

@SpringBootTest(classes = { SwimDispatcherProperties.class, DispatcherUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class DispatcherUseCaseTest {
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private FileDispatchingOutPort fileDispatchingOutPort;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoBean
    private FileHandlingHelper fileHandlingHelper;
    @MockitoSpyBean
    @Autowired
    private DispatcherUseCase dispatcherUseCase;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    @Test
    void testTriggerDispatching_Success() throws FileSizeException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFiles(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(List.of(FILE1, FILE2));
        doNothing().when(dispatcherUseCase).processFile(any(), any());
        // call
        dispatcherUseCase.triggerDispatching();
        // test
        verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE1));
        verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE2));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendDispatchErrors(any(), any(), any());
    }

    @Test
    void testTriggerDispatching_Exception() throws FileSizeException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFiles(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(List.of(FILE1, FILE2));
        final FileSizeException e = new FileSizeException("Error");
        doThrow(e).when(dispatcherUseCase).processFile(any(), any());
        // call
        dispatcherUseCase.triggerDispatching();
        // test
        verify(fileHandlingHelper, times(1)).markFileError(eq(FILE1), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
        verify(fileHandlingHelper, times(1)).markFileError(eq(FILE2), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
        verify(notificationOutPort, times(1)).sendDispatchErrors(eq(USE_CASE_RECIPIENTS), eq(useCase.getName()), eq(Map.of(
                FILE1.path(), e,
                FILE2.path(), e)));
    }

    @Test
    void testProcessFile_Success() throws FileSizeException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(BUCKET), eq("test/inProcess/path/test.json"))).thenReturn(true);
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq("test/inProcess/path/test.json"))).thenReturn("presignedMeta");
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(FILE1.path()))).thenReturn("presignedFile");
        // call
        dispatcherUseCase.processFile(useCase, FILE1);
        // test
        verify(fileDispatchingOutPort, times(1)).dispatchFile(eq(useCase.getDestinationBinding()), eq(USE_CASE), eq("presignedFile"), eq("presignedMeta"));
        verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
    }

    @Test
    void testProcessFile_FileSizeException() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        assertThrows(FileSizeException.class,
                () -> dispatcherUseCase.processFile(useCase, new File(BUCKET, "test.pdf", swimDispatcherProperties.getMaxFileSize() + 1)));
    }

    @Test
    void testProcessFile_MetadataException() {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(BUCKET), eq("test/inProcess/path/test.json"))).thenReturn(false);
        // call and test
        assertThrows(MetadataException.class, () -> dispatcherUseCase.processFile(useCase, FILE1));
    }
}

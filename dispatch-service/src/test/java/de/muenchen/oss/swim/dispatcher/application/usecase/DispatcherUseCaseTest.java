package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FOLDER_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TAGS;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_DISPATCH_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_RECIPIENTS;
import static de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction.IGNORE;
import static de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction.REROUTE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.helper.MetadataHelper;
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

@SpringBootTest(classes = { SwimDispatcherProperties.class, DispatcherUseCase.class, FileHandlingHelper.class, ObjectMapper.class, MetadataHelper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class DispatcherUseCaseTest {
    public static final String PRESIGNED_URL_METADATA_FILE = "presignedMeta";
    public static final String PRESIGNED_URL_FILE = "presignedFile";
    public static final String METATA_PATH = "test/inProcess/path/test.json";

    @MockitoBean
    private DispatchMeter dispatchMeter;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private FileDispatchingOutPort fileDispatchingOutPort;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoSpyBean
    @Autowired
    private FileHandlingHelper fileHandlingHelper;
    @MockitoSpyBean
    @Autowired
    private DispatcherUseCase dispatcherUseCase;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    private static final Map<File, Map<String, String>> FILE_LIST = Map.of(
            FILE1, TAGS,
            FILE2, TAGS);

    @Test
    void testTriggerDispatching_Success() throws FileSizeException, MetadataException, UseCaseException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(FILE_LIST);
        doNothing().when(dispatcherUseCase).processFile(any(), any(), any());
        // call
        dispatcherUseCase.triggerDispatching();
        // test
        verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE1), eq(TAGS));
        verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE2), eq(TAGS));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendDispatchErrors(any(), any(), any());
    }

    @Test
    void testTriggerDispatching_Exception() throws FileSizeException, MetadataException, UseCaseException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(FILE_LIST);
        final FileSizeException e = new FileSizeException("Error");
        doThrow(e).when(dispatcherUseCase).processFile(any(), any(), any());
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
    void testProcessFile_Success() throws FileSizeException, MetadataException, UseCaseException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(BUCKET), eq(METATA_PATH))).thenReturn(true);
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(METATA_PATH))).thenReturn(PRESIGNED_URL_METADATA_FILE);
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(FILE1.path()))).thenReturn(PRESIGNED_URL_FILE);
        // call
        dispatcherUseCase.processFile(useCase, FILE1, TAGS);
        // test
        verify(fileDispatchingOutPort, times(1)).dispatchFile(eq(useCase.getDestinationBinding()), eq(USE_CASE), eq(PRESIGNED_URL_FILE), eq(
                PRESIGNED_URL_METADATA_FILE));
        verify(dispatcherUseCase, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
        verify(dispatchMeter, times(1)).incrementDispatched(eq(USE_CASE), eq(useCase.getDestinationBinding()));
    }

    @Test
    void testProcessFile_MetadataDestination() throws FileSizeException, MetadataException, UseCaseException {
        // setup
        final String useCaseName = "test-meta-dest";
        final UseCase useCase = swimDispatcherProperties.findUseCase(useCaseName);
        when(fileSystemOutPort.fileExists(eq(BUCKET), eq(METATA_PATH))).thenReturn(true);
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(METATA_PATH))).thenReturn(PRESIGNED_URL_METADATA_FILE);
        when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(FILE1.path()))).thenReturn(PRESIGNED_URL_FILE);
        when(fileSystemOutPort.readFile(eq(BUCKET), eq(METATA_PATH)))
                .thenReturn(getClass().getResourceAsStream("/files/example-metadata-destination.json"));
        // call
        dispatcherUseCase.processFile(useCase, FILE1, TAGS);
        // test
        verify(fileDispatchingOutPort, times(1)).dispatchFile(eq("invoice-out"), eq(useCaseName), eq(PRESIGNED_URL_FILE), eq(PRESIGNED_URL_METADATA_FILE));
        verify(dispatcherUseCase, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
        verify(dispatchMeter, times(1)).incrementDispatched(eq(useCaseName), eq("invoice-out"));
    }

    @Test
    void testProcessFile_FileSizeException() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        assertThrows(FileSizeException.class,
                () -> dispatcherUseCase.processFile(useCase, new File(BUCKET, "test.pdf", swimDispatcherProperties.getMaxFileSize() + 1), TAGS));
    }

    @Test
    void testProcessFile_MetadataException() {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(BUCKET), eq(METATA_PATH))).thenReturn(false);
        // call and test
        assertThrows(MetadataException.class, () -> dispatcherUseCase.processFile(useCase, FILE1, TAGS));
    }

    @Test
    void testProcessFile_ActionIgnore() throws FileSizeException, UseCaseException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final Map<String, String> tags = Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue(),
                swimDispatcherProperties.getDispatchActionTagKey(), "ignore");
        // call
        dispatcherUseCase.processFile(useCase, FILE1, tags);
        // test
        verify(fileDispatchingOutPort, times(0)).dispatchFile(any(), any(), any(), any());
        verify(dispatcherUseCase, times(0)).dispatchFile(any(), any(), any());
        verify(dispatcherUseCase, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchFileFinishedTagValue())));
        verify(dispatchMeter, times(1)).incrementDispatched(eq(USE_CASE), eq(IGNORE.name()));
    }

    @Test
    void testProcessFile_ActionReroute() throws FileSizeException, UseCaseException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final Map<String, String> tags = Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue(),
                swimDispatcherProperties.getDispatchActionTagKey(), "reroute",
                DispatcherUseCase.ACTION_REROUTE_DESTINATION_TAG_KEY, "test2");
        // call
        dispatcherUseCase.processFile(useCase, FILE1, tags);
        // test
        verify(fileDispatchingOutPort, times(0)).dispatchFile(any(), any(), any(), any());
        verify(dispatcherUseCase, times(1)).rerouteFileToUseCase(eq(useCase), eq(FILE1), eq(tags));
        verify(fileSystemOutPort, times(1)).copyFile(eq(BUCKET), eq(FILE1.path()), eq("test-bucket-2"),
                eq("path/test2/inProcess/from_test-meta/path/test.pdf"), eq(true));
        verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchFileFinishedTagValue())));
        verify(dispatchMeter, times(1)).incrementDispatched(eq(USE_CASE), eq(REROUTE.name()));
    }
}

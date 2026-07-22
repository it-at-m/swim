package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1_BASE_NAME;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1_GROUP;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2_BASE_NAME;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2_GROUP;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE_LIST;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FOLDER_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.GROUPED_FILE_LIST;
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

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.DispatchActionsHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.GroupingHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.ValidationHelper;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileChunkException;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.helper.MetadataHelper;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = { SwimDispatcherProperties.class, DispatcherUseCase.class, FileHandlingHelper.class, JsonMapper.class, MetadataHelper.class,
                GroupingHelper.class, DispatchActionsHelper.class, ValidationHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class DispatcherUseCaseTest {
    public static final String PRESIGNED_URL_METADATA_FILE = "presignedMeta";
    public static final String PRESIGNED_URL_FILE = "presignedFile";
    public static final PresignedFile PRESIGNED_FILE = new PresignedFile(PRESIGNED_URL_FILE, PRESIGNED_URL_METADATA_FILE);
    public static final String METADATA_PATH = "test/inProcess/path/test.json";
    public static final FileReference METADATA_FILE = new FileReference(BUCKET, METADATA_PATH);

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
    private DispatchActionsHelper dispatchActionsHelper;
    @MockitoSpyBean
    @Autowired
    private GroupingHelper groupingHelper;
    @MockitoSpyBean
    @Autowired
    private ValidationHelper validationHelper;
    @MockitoSpyBean
    @Autowired
    private DispatcherUseCase dispatcherUseCase;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    @Test
    void testTriggerDispatching_Success() throws MetadataException, UseCaseException, FileChunkException, FileSizeException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(FILE_LIST);
        doNothing().when(dispatcherUseCase).processFileGroup(any(), any(), any());
        // call
        dispatcherUseCase.triggerDispatching();
        // test
        verify(groupingHelper).groupFiles(eq(FILE_LIST));
        verify(validationHelper).validateFileGroup(eq(useCase), eq(FILE1_BASE_NAME), eq(FILE1_GROUP));
        verify(validationHelper).validateFileGroup(eq(useCase), eq(FILE2_BASE_NAME), eq(FILE2_GROUP));
        verify(dispatcherUseCase).processFileGroup(eq(useCase), eq(FILE1_BASE_NAME), eq(GROUPED_FILE_LIST.get(FILE1_BASE_NAME)));
        verify(dispatcherUseCase).processFileGroup(eq(useCase), eq(FILE2_BASE_NAME), eq(GROUPED_FILE_LIST.get(FILE2_BASE_NAME)));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendDispatchErrors(any(), any(), any());
    }

    @Test
    void testTriggerDispatching_Exception() throws MetadataException, UseCaseException, FileChunkException, FileSizeException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH))).thenReturn(List.of(FOLDER_PATH));
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(FILE_LIST);
        final MetadataException e = new MetadataException("Error");
        doThrow(e).when(dispatcherUseCase).processFileGroup(any(), any(), any());
        // call
        dispatcherUseCase.triggerDispatching();
        // test
        verify(groupingHelper).groupFiles(eq(FILE_LIST));
        verify(validationHelper).validateFileGroup(eq(useCase), eq(FILE1_BASE_NAME), eq(FILE1_GROUP));
        verify(validationHelper).validateFileGroup(eq(useCase), eq(FILE2_BASE_NAME), eq(FILE2_GROUP));
        verify(fileHandlingHelper).markFileError(eq(FILE1.reference()), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
        verify(fileHandlingHelper).markFileError(eq(FILE2.reference()), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
        verify(notificationOutPort).sendDispatchErrors(eq(USE_CASE_RECIPIENTS), eq(useCase.getName()), eq(Map.of(
                FILE1.reference(), e,
                FILE2.reference(), e)));
    }

    @Test
    void testProcessFile_Group_Success() throws MetadataException, UseCaseException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(METADATA_FILE))).thenReturn(true);
        when(fileSystemOutPort.getPresignedUrl(eq(METADATA_FILE))).thenReturn(PRESIGNED_URL_METADATA_FILE);
        when(fileSystemOutPort.getPresignedUrl(eq(FILE1.reference()))).thenReturn(PRESIGNED_URL_FILE);
        // call
        dispatcherUseCase.processFileGroup(useCase, FILE1.reference().getFileNameWithoutExtension(), FILE1_GROUP);
        // test
        verify(fileDispatchingOutPort).dispatchFile(eq(useCase.getDestinationBinding()), eq(USE_CASE), eq(PRESIGNED_FILE));
        verify(dispatchActionsHelper, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileSystemOutPort).tagFile(eq(FILE1.reference()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
        verify(dispatchMeter).incrementDispatched(eq(USE_CASE), eq(useCase.getDestinationBinding()));
    }

    @Test
    void testProcessFile_Group_MetadataDestination() throws MetadataException, UseCaseException {
        // setup
        final String useCaseName = "test-meta-dest";
        final UseCase useCase = swimDispatcherProperties.findUseCase(useCaseName);
        when(fileSystemOutPort.fileExists(eq(METADATA_FILE))).thenReturn(true);
        when(fileSystemOutPort.getPresignedUrl(eq(METADATA_FILE))).thenReturn(PRESIGNED_URL_METADATA_FILE);
        when(fileSystemOutPort.getPresignedUrl(eq(FILE1.reference()))).thenReturn(PRESIGNED_URL_FILE);
        when(fileSystemOutPort.readFile(eq(METADATA_FILE)))
                .thenReturn(getClass().getResourceAsStream("/files/example-metadata-destination.json"));
        // call
        dispatcherUseCase.processFileGroup(useCase, FILE1_BASE_NAME, FILE1_GROUP);
        // test
        verify(fileDispatchingOutPort).dispatchFile(eq("invoice-out"), eq(useCaseName), eq(PRESIGNED_FILE));
        verify(dispatchActionsHelper, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileSystemOutPort).tagFile(eq(FILE1.reference()), eq(Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
        verify(dispatchMeter).incrementDispatched(eq(useCaseName), eq("invoice-out"));
    }

    @Test
    void testProcessFile_Group_MetadataException() {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        when(fileSystemOutPort.fileExists(eq(METADATA_FILE))).thenReturn(false);
        // call and test
        assertThrows(MetadataException.class, () -> dispatcherUseCase.processFileGroup(useCase, FILE1_BASE_NAME, FILE1_GROUP));
    }

    @Test
    void testProcessFile_Group_ActionIgnore() throws UseCaseException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final Map<String, String> tags = Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue(),
                swimDispatcherProperties.getDispatchActionTagKey(), "ignore");
        final FileWithMetadata file = new FileWithMetadata(FILE1.reference(), 0, null, tags);
        // call
        dispatcherUseCase.processFileGroup(useCase, FILE1_BASE_NAME, new FileGroup(false, file));
        // test
        verify(fileDispatchingOutPort, times(0)).dispatchFile(any(), any(), (PresignedFile) any());
        verify(dispatchActionsHelper, times(0)).dispatchFileGroup(any(), any(), any());
        verify(dispatchActionsHelper, times(0)).rerouteFileToUseCase(any(), any(), any());
        verify(fileHandlingHelper).finishFile(eq(useCase), eq(FILE1.reference()));
        verify(dispatchMeter).incrementDispatched(eq(USE_CASE), eq(IGNORE.name()));
    }

    @Test
    void testProcessFile_Group_ActionReroute() throws UseCaseException, MetadataException {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final Map<String, String> tags = Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue(),
                swimDispatcherProperties.getDispatchActionTagKey(), "reroute",
                "SWIM_Reroute_Destination", "test2");
        final FileWithMetadata file = new FileWithMetadata(FILE1.reference(), 0, null, tags);
        // call
        dispatcherUseCase.processFileGroup(useCase, FILE1_BASE_NAME, new FileGroup(false, file));
        // test
        final FileReference destFile = new FileReference("test-bucket-2", "path/test2/inProcess/from_test-meta/path/test.pdf");
        verify(fileDispatchingOutPort, times(0)).dispatchFile(any(), any(), (PresignedFile) any());
        verify(dispatchActionsHelper).rerouteFileToUseCase(eq(useCase), eq(FILE1.reference()), eq(tags));
        verify(fileSystemOutPort).copyFile(eq(FILE1.reference()), eq(destFile), eq(true));
        verify(fileSystemOutPort).tagFile(eq(destFile), eq(Map.of(
                "SWIM_State", "protocolProcessingSuccessful")));
        verify(fileHandlingHelper).finishFile(eq(useCase), eq(FILE1.reference()));
        verify(dispatchMeter).incrementDispatched(eq(USE_CASE), eq(REROUTE.name()));
    }
}

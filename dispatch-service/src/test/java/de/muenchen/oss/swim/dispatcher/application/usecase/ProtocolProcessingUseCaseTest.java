package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE2;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TAGS;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_DISPATCH_PATH;
import static de.muenchen.oss.swim.dispatcher.TestConstants.USE_CASE_RECIPIENTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.StoreProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.io.InputStream;
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

@SpringBootTest(classes = { SwimDispatcherProperties.class, ProtocolProcessingUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@SuppressWarnings({ "CPD-START", "PMD.CloseResource" })
class ProtocolProcessingUseCaseTest {
    public static final String EXAMPLE_PROTOCOL_RESOURCE_PATH = "files/protocol.csv";

    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private ReadProtocolOutPort readProtocolOutPort;
    @MockitoBean
    private StoreProtocolOutPort storeProtocolOutPort;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoBean
    private FileHandlingHelper fileHandlingHelper;
    @MockitoSpyBean
    @Autowired
    private ProtocolProcessingUseCase protocolProcessingUseCase;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    private static final FileWithMetadata PROTOCOL_FILE = new FileWithMetadata(new FileReference(BUCKET, "test/inProcess/path/path.csv"), 0L, null, TAGS);
    private static final FileWithMetadata NO_PROTOCOL_FILE = new FileWithMetadata(new FileReference(BUCKET, "test/inProcess/path/path2.csv"), 0L, null,
            TAGS);
    private static final String PROTOCOL_RAW_PATH = "path/path.csv";
    private static final ProtocolEntry PROTOCOL_ENTRY1 = new ProtocolEntry("test.pdf", 1, null, null, null, null, null, Map.of());
    private static final ProtocolEntry PROTOCOL_ENTRY2 = new ProtocolEntry("test2.pdf", 2, null, null, null, null, null, Map.of());

    @Test
    void testTriggerProtocolProcessing_Successful() {
        // setup
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH), eq(true), eq("csv"), anyMap(), anyMap())).thenReturn(List.of(
                PROTOCOL_FILE, NO_PROTOCOL_FILE));
        doNothing().when(protocolProcessingUseCase).processProtocolFile(any(), any());
        // call
        protocolProcessingUseCase.triggerProtocolProcessing();
        // test
        verify(fileSystemOutPort).getMatchingFilesWithTags(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH), anyBoolean(), any(), any(), anyMap());
        verify(protocolProcessingUseCase).processProtocolFile(any(), any());
        verify(protocolProcessingUseCase).processProtocolFile(any(), eq(PROTOCOL_FILE.reference()));
        verify(notificationOutPort).sendProtocolError(any(), eq(USE_CASE), eq(NO_PROTOCOL_FILE.reference().path()), any());
    }

    @Test
    void testProcessProtocolFile_Successful() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // setup
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.reference())))
                .thenReturn(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2));
        final String protocolDir = PROTOCOL_FILE.reference().getParentPath();
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(protocolDir), eq(false), any(), any(), anyMap())).thenReturn(List.of(FILE1));
        final String protocolDirFinished = useCase.getFinishedPath(swimDispatcherProperties, protocolDir);
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(protocolDirFinished), eq(false), any(), any(), anyMap()))
                .thenReturn(List.of(FILE1, FILE2));
        final InputStream protocolStream = getClass().getResourceAsStream(EXAMPLE_PROTOCOL_RESOURCE_PATH);
        when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.reference()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE.reference());
        // test
        verify(notificationOutPort).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(protocolStream), eq(List.of()),
                eq(List.of()));
        verify(storeProtocolOutPort).deleteProtocol(eq(USE_CASE), eq(PROTOCOL_RAW_PATH));
        verify(storeProtocolOutPort).storeProtocol(eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2)));
        verify(fileSystemOutPort).tagFile(eq(PROTOCOL_FILE.reference()), eq(Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), "correct")));
        verify(fileSystemOutPort).moveFile(eq(PROTOCOL_FILE.reference()), eq("test/finishedProtocols/path/path.csv"));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
    }

    @Test
    void testProcessProtocolFile_Missmatch() {
        // setup
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.reference()))).thenReturn(List.of(
                PROTOCOL_ENTRY1,
                PROTOCOL_ENTRY2,
                new ProtocolEntry("test4.pdf", 1, null, null, null, null, null, Map.of())));
        when(fileSystemOutPort.getMatchingFilesWithTags(any(), any(), anyBoolean(), any(), any(), anyMap())).thenReturn(List.of(
                FILE1,
                FILE2,
                new FileWithMetadata(new FileReference(BUCKET, "test/inProcess/path/test3.pdf"), 0L, null, TAGS)));
        final InputStream protocolStream = getClass().getResourceAsStream(EXAMPLE_PROTOCOL_RESOURCE_PATH);
        when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.reference()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE.reference());
        // test
        verify(notificationOutPort).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(protocolStream),
                eq(List.of("test4.pdf")), eq(List.of("test3.pdf")));
        verify(fileSystemOutPort).tagFile(eq(PROTOCOL_FILE.reference()), eq(Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), "missingInProtocolAndFiles")));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
    }

    @Test
    void testProcessProtocolFile_ProtocolException() {
        // setup
        final ProtocolException e = new ProtocolException("Error", new Exception());
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.reference()))).thenThrow(e);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE.reference());
        // test
        verify(fileHandlingHelper).markFileError(eq(PROTOCOL_FILE.reference()), eq(swimDispatcherProperties.getProtocolStateTagKey()), eq(e));
        verify(notificationOutPort).sendProtocolError(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_FILE.reference().path()), eq(e));
    }

    @Test
    void testProcessProtocolFile_IgnorePattern() throws UseCaseException {
        final UseCase useCase = swimDispatcherProperties.findUseCase("test-meta-dest");
        final FileWithMetadata protocolFile = new FileWithMetadata(new FileReference(BUCKET, "test3/inProcess/path/path.csv"), 0L, null, TAGS);
        // setup
        when(readProtocolOutPort.loadProtocol(eq(protocolFile.reference()))).thenReturn(List.of(
                new ProtocolEntry("test.pdf", 1, null, null, null, null, null, Map.of())));
        when(fileSystemOutPort.getMatchingFilesWithTags(any(), any(), anyBoolean(), any(), any(), anyMap())).thenReturn(List.of(
                new FileWithMetadata(new FileReference(BUCKET, "test3/inProcess/path/test.pdf"), 0L, null, TAGS),
                new FileWithMetadata(new FileReference(BUCKET, "test3/inProcess/path/test-1v1.pdf"), 0L, null, TAGS),
                new FileWithMetadata(new FileReference(BUCKET, "test3/inProcess/path/test-1v1-wrong.pdf"), 0L, null, TAGS)));
        final InputStream protocolStream = getClass().getResourceAsStream(EXAMPLE_PROTOCOL_RESOURCE_PATH);
        when(fileSystemOutPort.readFile(eq(protocolFile.reference()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(useCase, protocolFile.reference());
        // test
        verify(notificationOutPort).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(useCase.getName()), eq(PROTOCOL_RAW_PATH), eq(protocolStream),
                eq(List.of()), eq(List.of("test-1v1-wrong.pdf")));
        verify(fileSystemOutPort).tagFile(eq(protocolFile.reference()), eq(Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), "missingInProtocol")));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
    }

    @Test
    void testProcessProtocolFile_TagProcessedFiles() throws UseCaseException {
        // use case with tag-protocol-processed enabled
        final UseCase useCase = swimDispatcherProperties.findUseCase("test2");
        final FileWithMetadata protocolFile = new FileWithMetadata(new FileReference("test-bucket-2", "path/test2/inProcess/path/path.csv"), 0L, null, TAGS);
        final FileWithMetadata inProcessFile1 = new FileWithMetadata(new FileReference("test-bucket-2", "path/test2/inProcess/path/test.pdf"), 0L, null, TAGS);
        final FileWithMetadata inProcessFile2 = new FileWithMetadata(new FileReference("test-bucket-2", "path/test2/inProcess/path/test2.pdf"), 0L, null, TAGS);
        // setup
        when(readProtocolOutPort.loadProtocol(eq(protocolFile.reference()))).thenReturn(List.of(
                new ProtocolEntry("test.pdf", 1, null, null, null, null, null, Map.of()),
                new ProtocolEntry("test2.pdf", 2, null, null, null, null, null, Map.of())));
        final String protocolDir = protocolFile.reference().getParentPath();
        // in-process contains both files
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(protocolFile.reference().bucket()), eq(protocolDir), eq(false), any(), any(), anyMap()))
                .thenReturn(List.of(
                        inProcessFile1, inProcessFile2));
        // finished dir empty
        final String protocolDirFinished = useCase.getFinishedPath(swimDispatcherProperties, protocolDir);
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(protocolFile.reference().bucket()), eq(protocolDirFinished), eq(false), any(), any(), anyMap()))
                .thenReturn(List.of());
        final InputStream protocolStream = getClass().getResourceAsStream(EXAMPLE_PROTOCOL_RESOURCE_PATH);
        when(fileSystemOutPort.readFile(eq(protocolFile.reference()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(useCase, protocolFile.reference());
        // test: both in-process files should be tagged as protocol processed
        verify(fileSystemOutPort).tagFile(eq(inProcessFile1.reference()), eq(Map.of(
                "SWIM_State", "protocolProcessingSuccessful")));
        verify(fileSystemOutPort).tagFile(eq(inProcessFile2.reference()), eq(Map.of(
                "SWIM_State", "protocolProcessingSuccessful")));
    }
}

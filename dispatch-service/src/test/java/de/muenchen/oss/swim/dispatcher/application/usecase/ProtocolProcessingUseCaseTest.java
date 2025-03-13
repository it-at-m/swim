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
import de.muenchen.oss.swim.dispatcher.domain.model.File;
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
@SuppressWarnings("CPD-START")
class ProtocolProcessingUseCaseTest {
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

    private static final File PROTOCOL_FILE = new File(BUCKET, "test/inProcess/path/path.csv", 0L);
    private static final File NO_PROTOCOL_FILE = new File(BUCKET, "test/inProcess/path/path2.csv", 0L);
    private static final String PROTOCOL_RAW_PATH = "path/path.csv";
    private static final ProtocolEntry PROTOCOL_ENTRY1 = new ProtocolEntry("test.pdf", 1, null, null, null, null, null, Map.of());
    private static final ProtocolEntry PROTOCOL_ENTRY2 = new ProtocolEntry("test2.pdf", 2, null, null, null, null, null, Map.of());

    @Test
    void testTriggerProtocolProcessing_Successful() {
        // setup
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH), eq(true), eq("csv"), anyMap(), anyMap())).thenReturn(Map.of(
                PROTOCOL_FILE, TAGS,
                NO_PROTOCOL_FILE, TAGS));
        doNothing().when(protocolProcessingUseCase).processProtocolFile(any(), any());
        // call
        protocolProcessingUseCase.triggerProtocolProcessing();
        // test
        verify(fileSystemOutPort, times(1)).getMatchingFilesWithTags(eq(BUCKET), eq(USE_CASE_DISPATCH_PATH), anyBoolean(), any(), any(), anyMap());
        verify(protocolProcessingUseCase, times(1)).processProtocolFile(any(), any());
        verify(protocolProcessingUseCase, times(1)).processProtocolFile(any(), eq(PROTOCOL_FILE));
        verify(notificationOutPort, times(1)).sendProtocolError(any(), eq(USE_CASE), eq(NO_PROTOCOL_FILE.path()), any());
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void testProcessProtocolFile_Successful() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // setup
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2));
        final String protocolDir = PROTOCOL_FILE.getParentPath();
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(protocolDir), eq(false), any(), any(), anyMap())).thenReturn(Map.of(FILE1, TAGS));
        final String protocolDirFinished = useCase.getFinishedPath(swimDispatcherProperties, protocolDir);
        when(fileSystemOutPort.getMatchingFilesWithTags(eq(BUCKET), eq(protocolDirFinished), eq(false), any(), any(), anyMap()))
                .thenReturn(Map.of(FILE1, TAGS, FILE2, TAGS));
        final InputStream protocolStream = getClass().getResourceAsStream("files/protocol.csv");
        when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
        // test
        verify(notificationOutPort, times(1)).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(protocolStream), eq(List.of()),
                eq(List.of()));
        verify(storeProtocolOutPort, times(1)).deleteProtocol(eq(USE_CASE), eq(PROTOCOL_RAW_PATH));
        verify(storeProtocolOutPort, times(1)).storeProtocol(eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2)));
        verify(fileSystemOutPort, times(1)).tagFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()), eq(Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), "correct")));
        verify(fileSystemOutPort, times(1)).moveFile(eq(BUCKET), eq(PROTOCOL_FILE.path()), eq("test/finishedProtocols/path/path.csv"));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void testProcessProtocolFile_Missmatch() {
        // setup
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(List.of(
                PROTOCOL_ENTRY1,
                PROTOCOL_ENTRY2,
                new ProtocolEntry("test4.pdf", 1, null, null, null, null, null, Map.of())));
        when(fileSystemOutPort.getMatchingFilesWithTags(any(), any(), anyBoolean(), any(), any(), anyMap())).thenReturn(Map.of(
                FILE1, TAGS,
                FILE2, TAGS,
                new File(BUCKET, "test/inProcess/path/test3.pdf", 0L), TAGS));
        final InputStream protocolStream = getClass().getResourceAsStream("files/protocol.csv");
        when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(protocolStream);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
        // test
        verify(notificationOutPort, times(1)).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_RAW_PATH), eq(protocolStream),
                eq(List.of("test4.pdf")), eq(List.of("test3.pdf")));
        verify(fileSystemOutPort, times(1)).tagFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()), eq(Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), "missingInProtocolAndFiles")));
        verify(fileHandlingHelper, times(0)).markFileError(any(), any(), any());
        verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
    }

    @Test
    void testProcessProtocolFile_ProtocolException() {
        // setup
        final ProtocolException e = new ProtocolException("Error", new Exception());
        when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenThrow(e);
        // call
        protocolProcessingUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
        // test
        verify(fileHandlingHelper, times(1)).markFileError(eq(PROTOCOL_FILE), eq(swimDispatcherProperties.getProtocolStateTagKey()), eq(e));
        verify(notificationOutPort, times(1)).sendProtocolError(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_FILE.path()), eq(e));
    }
}

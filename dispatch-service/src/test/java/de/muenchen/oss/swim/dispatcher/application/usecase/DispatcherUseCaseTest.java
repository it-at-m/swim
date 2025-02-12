package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.StoreProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
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
    private DispatchMeter dispatchMeter;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private FileDispatchingOutPort fileDispatchingOutPort;
    @MockitoBean
    private ReadProtocolOutPort readProtocolOutPort;
    @MockitoBean
    private StoreProtocolOutPort storeProtocolOutPort;
    @MockitoBean
    private NotificationOutPort notificationOutPort;
    @MockitoSpyBean
    @Autowired
    private DispatcherUseCase dispatcherUseCase;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    private static final String USE_CASE = "test-meta";
    private static final List<String> USE_CASE_RECIPIENTS = List.of("test-meta@example.com");
    private static final String USE_CASE_PATH = "test";
    private static final String FOLDER_PATH = "test/path/";
    private static final File FILE1 = new File(BUCKET, "test/path/test.pdf", 0L);
    private static final File FILE2 = new File(BUCKET, "test/path/test2.pdf", 0L);

    @Nested
    class FileDispatchingTest {
        @Test
        void testTriggerDispatching_Success() throws FileSizeException, MetadataException {
            // setup
            final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
            when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_PATH))).thenReturn(List.of(FOLDER_PATH));
            when(fileSystemOutPort.getMatchingFiles(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(List.of(FILE1, FILE2));
            doNothing().when(dispatcherUseCase).processFile(any(), any());
            // call
            dispatcherUseCase.triggerDispatching();
            // test
            verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE1));
            verify(dispatcherUseCase, times(1)).processFile(eq(useCase), eq(FILE2));
            verify(dispatcherUseCase, times(0)).markFileError(any(), any(), any());
            verify(notificationOutPort, times(0)).sendDispatchErrors(any(), any(), any());
        }

        @Test
        void testTriggerDispatching_Exception() throws FileSizeException, MetadataException {
            // setup
            final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
            when(fileSystemOutPort.getSubDirectories(eq(BUCKET), eq(USE_CASE_PATH))).thenReturn(List.of(FOLDER_PATH));
            when(fileSystemOutPort.getMatchingFiles(eq(BUCKET), eq(FOLDER_PATH), eq(true), eq("pdf"), anyMap(), anyMap())).thenReturn(List.of(FILE1, FILE2));
            final FileSizeException e = new FileSizeException("Error");
            doThrow(e).when(dispatcherUseCase).processFile(any(), any());
            // call
            dispatcherUseCase.triggerDispatching();
            // test
            verify(dispatcherUseCase, times(1)).markFileError(eq(FILE1), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
            verify(dispatcherUseCase, times(1)).markFileError(eq(FILE2), eq(swimDispatcherProperties.getDispatchStateTagKey()), eq(e));
            verify(notificationOutPort, times(1)).sendDispatchErrors(eq(USE_CASE_RECIPIENTS), eq(useCase.getName()), eq(Map.of(
                    FILE1.path(), e,
                    FILE2.path(), e)));
        }

        @Test
        void testProcessFile_Success() throws FileSizeException, MetadataException {
            // setup
            final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
            when(fileSystemOutPort.fileExists(eq(BUCKET), eq("test/path/test.json"))).thenReturn(true);
            when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq("test/path/test.json"))).thenReturn("presignedMeta");
            when(fileSystemOutPort.getPresignedUrl(eq(BUCKET), eq(FILE1.path()))).thenReturn("presignedFile");
            // call
            dispatcherUseCase.processFile(useCase, FILE1);
            // test
            verify(fileDispatchingOutPort, times(1)).dispatchFile(eq(useCase.getDestinationBinding()), eq(USE_CASE), eq("presignedFile"), eq("presignedMeta"));
            verify(fileSystemOutPort, times(1)).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                    swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchedStateTagValue())));
            verify(dispatchMeter, times(1)).incrementDispatched(eq(USE_CASE), eq(useCase.getDestinationBinding()));
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
            when(fileSystemOutPort.fileExists(eq(BUCKET), eq("test/path/test.json"))).thenReturn(false);
            // call and test
            assertThrows(MetadataException.class, () -> dispatcherUseCase.processFile(useCase, FILE1));
        }
    }

    @Nested
    class ProtocolProcessingTest {
        private static final File PROTOCOL_FILE = new File(BUCKET, "test/path/path.csv", 0L);
        private static final File NO_PROTOCOL_FILE = new File(BUCKET, "test/path/path2.csv", 0L);
        private static final String PROTOCOL_FILENAME = "path.csv";
        private static final ProtocolEntry PROTOCOL_ENTRY1 = new ProtocolEntry("test.pdf", 1, null, null, null, Map.of());
        private static final ProtocolEntry PROTOCOL_ENTRY2 = new ProtocolEntry("test2.pdf", 2, null, null, null, Map.of());

        @Test
        void testTriggerProtocolProcessing_Successful() {
            // setup
            when(fileSystemOutPort.getMatchingFiles(eq(BUCKET), eq(USE_CASE_PATH), eq(true), eq("csv"), anyMap(), anyMap())).thenReturn(List.of(
                    PROTOCOL_FILE,
                    NO_PROTOCOL_FILE));
            doNothing().when(dispatcherUseCase).processProtocolFile(any(), any());
            // call
            dispatcherUseCase.triggerProtocolProcessing();
            // test
            verify(fileSystemOutPort, times(1)).getMatchingFiles(any(), any(), anyBoolean(), any(), any(), anyMap());
            verify(dispatcherUseCase, times(1)).processProtocolFile(any(), any());
            verify(dispatcherUseCase, times(1)).processProtocolFile(any(), eq(PROTOCOL_FILE));
            verify(notificationOutPort, times(1)).sendProtocolError(any(), eq(USE_CASE), eq(NO_PROTOCOL_FILE.path()), any());
        }

        @Test
        @SuppressWarnings("PMD.CloseResource")
        void testProcessProtocolFile_Successful() {
            // setup
            when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2));
            when(fileSystemOutPort.getMatchingFiles(any(), any(), anyBoolean(), any(), any(), anyMap())).thenReturn(List.of(FILE1, FILE2));
            final InputStream protocolStream = getClass().getResourceAsStream("file/protocol.csv");
            when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(protocolStream);
            // call
            dispatcherUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
            // test
            verify(notificationOutPort, times(1)).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_FILENAME), eq(protocolStream), eq(List.of()),
                    eq(List.of()));
            verify(storeProtocolOutPort, times(1)).storeProtocol(eq(USE_CASE), eq(PROTOCOL_FILENAME), eq(List.of(PROTOCOL_ENTRY1, PROTOCOL_ENTRY2)));
            verify(fileSystemOutPort, times(1)).tagFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()), eq(Map.of(
                    swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTageValue())));
            verify(dispatcherUseCase, times(0)).markFileError(any(), any(), any());
            verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
        }

        @Test
        @SuppressWarnings("PMD.CloseResource")
        void testProcessProtocolFile_Missmatch() {
            // setup
            when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(List.of(
                    PROTOCOL_ENTRY1,
                    PROTOCOL_ENTRY2,
                    new ProtocolEntry("test4.pdf", 1, null, null, null, Map.of())));
            when(fileSystemOutPort.getMatchingFiles(any(), any(), anyBoolean(), any(), any(), anyMap())).thenReturn(List.of(
                    FILE1,
                    FILE2,
                    new File(BUCKET, "test/path/test3.pdf", 0L)));
            final InputStream protocolStream = getClass().getResourceAsStream("file/protocol.csv");
            when(fileSystemOutPort.readFile(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenReturn(protocolStream);
            // call
            dispatcherUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
            // test
            verify(notificationOutPort, times(1)).sendProtocol(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_FILENAME), eq(protocolStream),
                    eq(List.of("test4.pdf")), eq(List.of("test3.pdf")));
            verify(dispatcherUseCase, times(0)).markFileError(any(), any(), any());
            verify(notificationOutPort, times(0)).sendProtocolError(any(), any(), any(), any());
        }

        @Test
        void testProcessProtocolFile_ProtocolException() {
            // setup
            final ProtocolException e = new ProtocolException("Error", new Exception());
            when(readProtocolOutPort.loadProtocol(eq(PROTOCOL_FILE.bucket()), eq(PROTOCOL_FILE.path()))).thenThrow(e);
            // call
            dispatcherUseCase.processProtocolFile(swimDispatcherProperties.getUseCases().getFirst(), PROTOCOL_FILE);
            // test
            verify(dispatcherUseCase, times(1)).markFileError(eq(PROTOCOL_FILE), eq(swimDispatcherProperties.getProtocolStateTagKey()), eq(e));
            verify(notificationOutPort, times(1)).sendProtocolError(eq(USE_CASE_RECIPIENTS), eq(USE_CASE), eq(PROTOCOL_FILE.path()), eq(e));
        }
    }
}

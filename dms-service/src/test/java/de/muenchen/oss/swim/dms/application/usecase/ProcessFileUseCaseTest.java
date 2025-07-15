package de.muenchen.oss.swim.dms.application.usecase;

import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_INCOMING;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.usecase.helper.TargetResolverHelper;
import de.muenchen.oss.swim.dms.configuration.DmsMeter;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseIncoming;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
        classes = { SwimDmsProperties.class, ProcessFileUseCase.class, ObjectMapper.class, DmsMetadataHelper.class, PatternHelper.class,
                TargetResolverHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ProcessFileUseCaseTest {
    @MockitoSpyBean
    @Autowired
    private SwimDmsProperties swimDmsProperties;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private DmsOutPort dmsOutPort;
    @MockitoBean
    private FileEventOutPort fileEventOutPort;
    @MockitoBean
    private DmsMeter dmsMeter;
    @MockitoSpyBean
    @Autowired
    private DmsMetadataHelper dmsMetadataHelper;
    @MockitoSpyBean
    @Autowired
    private TargetResolverHelper targetResolverHelper;
    @MockitoSpyBean
    @Autowired
    private ProcessFileUseCase processFileUseCase;

    private final static String BUCKET = "test-bucket";
    private final static String FILE_NAME_WITHOUT_EXTENSION = "test-COO.123.123.123-asd";
    private final static String FILE_NAME = String.format("%s.pdf", FILE_NAME_WITHOUT_EXTENSION);
    private final static String FILE_PATH = String.format("test-path/%s", FILE_NAME);
    private final static File FILE = new File(BUCKET, FILE_PATH);
    private final static String FILE_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, FILE_PATH);
    private final static String METADAT_PATH = String.format("test-path/%s.json", FILE_NAME_WITHOUT_EXTENSION);
    private final static String METADATA_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, METADAT_PATH);
    private final static DmsTarget STATIC_DMS_TARGET = new DmsTarget("staticCoo", "staticUsername", "staticJobOe", "staticJobPosition");
    private final static DmsTarget FILENAME_DMS_TARGET = new DmsTarget("COO.123.123.123", "staticUsername", "staticJobOe", "staticJobPosition");
    public static final String PATTERN_VALUE_TEST = "test";
    public static final String OVERWRITTEN_INCOMING_NAME = PATTERN_VALUE_TEST;

    @BeforeEach
    void setup() throws PresignedUrlException {
        when(fileSystemOutPort.getPresignedUrlFile(eq(FILE_PRESIGNED_URL))).thenReturn(null);
    }

    @Test
    void testProcessFile_MetadataInbox() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "metadata-inbox";
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        // setup
        when(fileSystemOutPort.getPresignedUrlFile(eq(METADATA_PRESIGNED_URL))).thenReturn(getClass().getResourceAsStream("/files/example-metadata-user.json"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, METADATA_PRESIGNED_URL), FILE);
        // test
        verify(swimDmsProperties, times(2)).findUseCase(eq(useCaseName));
        verify(targetResolverHelper, times(1)).resolveTargetCoo(eq(UseCaseType.INBOX_CONTENT_OBJECT), any(), eq(useCase), eq(FILE));
        verify(dmsMetadataHelper, times(1)).resolveInboxDmsTarget(any());
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(FILE_NAME, null);
        verify(dmsOutPort, times(1)).createContentObjectInInbox(eq(METADATA_DMS_TARGET_USER), eq(contentObjectRequest), eq(null));
        verify(dmsMeter, times(1)).incrementProcessed(eq(useCaseName), eq("INBOX_CONTENT_OBJECT"));
    }

    @Test
    void testProcessFile_StaticInbox() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "static-inbox";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(FILE_NAME, PATTERN_VALUE_TEST);
        verify(dmsOutPort, times(1)).createContentObjectInInbox(eq(STATIC_DMS_TARGET), eq(contentObjectRequest), eq(null));
    }

    @Test
    void testProcessFile_StaticInboxIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "static-inbox-incoming";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(FILE_NAME_WITHOUT_EXTENSION, null,
                new DmsContentObjectRequest(FILE_NAME, null));
        verify(dmsOutPort, times(1)).createIncomingInInbox(eq(STATIC_DMS_TARGET), eq(incomingRequest), eq(null));
    }

    @Test
    void testProcessFile_DecodeGermanChars() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "static-inbox-incoming";
        final String fileNameWithoutExtension = "test#a#o#u#s#A#O#Utest";
        final String fileName = String.format("%s.pdf", fileNameWithoutExtension);
        final String filePath = String.format("test/%s", fileName);
        final File fileIn = new File(BUCKET, filePath);
        final File fileDecoded = new File(BUCKET, filePath.replaceFirst(fileNameWithoutExtension, "testäöüßÄÖÜtest"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), fileIn);
        // test
        verify(processFileUseCase).processInboxIncoming(eq(fileDecoded), any(), any(), any(), any());
    }

    @Test
    void testProcessFile_MetadataViaMetadata() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "metadata-metadata";
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        // setup
        when(fileSystemOutPort.getPresignedUrlFile(eq(METADATA_PRESIGNED_URL)))
                .thenReturn(getClass().getResourceAsStream("/files/example-metadata-target-type-incoming.json"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, METADATA_PRESIGNED_URL), FILE);
        // test
        verify(targetResolverHelper, times(1)).resolveTypeFromMetadataFile(any());
        verify(targetResolverHelper, times(1)).resolveTargetCoo(eq(UseCaseType.PROCEDURE_INCOMING), any(), eq(useCase), eq(FILE));
        verify(dmsMetadataHelper, times(1)).resolveIncomingDmsTarget(any());
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(FILE_NAME_WITHOUT_EXTENSION, null,
                new DmsContentObjectRequest(FILE_NAME, null));
        verify(dmsOutPort, times(1)).createProcedureIncoming(eq(METADATA_DMS_TARGET_INCOMING), eq(incomingRequest), eq(null));
    }

    @Test
    void testProcessFile_StaticIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "static-incoming";
        final String overwrittenIncomingName = "asd";
        final String overwrittenContentObjectName = "test-asd.pdf";
        // setup
        when(fileSystemOutPort.getPresignedUrlFile(eq(METADATA_PRESIGNED_URL)))
                .thenReturn(getClass().getResourceAsStream("/files/example-metadata-subject.json"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, METADATA_PRESIGNED_URL), FILE);
        // test
        final String incomingSubject = "Test_Value_1 (TestKey_1)\nTest_Value_2 (TestKey_2)";
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, STATIC_DMS_TARGET, overwrittenIncomingName, incomingSubject, overwrittenContentObjectName);
        verify(dmsOutPort, times(0)).getProcedureName(any());
        verify(dmsOutPort, times(0)).getIncomingCooByName(any(), any());
    }

    @Test
    void testProcessFile_FilenameIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "filename-incoming";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, FILENAME_DMS_TARGET, FILE_NAME_WITHOUT_EXTENSION, null, FILE_NAME);
    }

    @Test
    void testProcessFile_FilenameMapIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "map-incoming";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, FILENAME_DMS_TARGET, FILE_NAME_WITHOUT_EXTENSION, null, FILE_NAME);
        // call catch all
        final String fileNameWithoutExtension = "äasd";
        final String fileName = String.format("%s.pdf", fileNameWithoutExtension);
        final String filePath = String.format("test/%s", fileName);
        final File file = new File(BUCKET, filePath);
        final String presignedUrl = String.format("http://localhost:9001/%s/%s", BUCKET, filePath);
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        processFileUseCase.processFile(new FileEvent(useCaseName, presignedUrl, null), file);
        final DmsTarget dmsTarget = new DmsTarget("COO.321.321.321", useCase.getContext());
        // test catche all
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(fileNameWithoutExtension, null,
                new DmsContentObjectRequest(fileName, null));
        verify(dmsOutPort, times(1)).createProcedureIncoming(eq(dmsTarget), eq(incomingRequest), eq(null));
    }

    @Test
    void testProcessFile_FilenameNameIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "filename-name-incoming";
        when(dmsOutPort.findObjectsByName(eq(DmsResourceType.PROCEDURE), eq(PATTERN_VALUE_TEST), any())).thenReturn(List.of("COO.123.123.123"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, FILENAME_DMS_TARGET, FILE_NAME_WITHOUT_EXTENSION, null, FILE_NAME);
        verify(dmsOutPort, times(1)).findObjectsByName(eq(DmsResourceType.PROCEDURE), eq(PATTERN_VALUE_TEST), any());
    }

    @Test
    void testProcessFile_verifyProcedureName() throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        final String useCaseName = "verifyProcedure-incoming";
        // setup
        when(dmsOutPort.getProcedureName(eq(FILENAME_DMS_TARGET))).thenReturn(OVERWRITTEN_INCOMING_NAME);
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, FILENAME_DMS_TARGET, FILE_NAME_WITHOUT_EXTENSION, null, FILE_NAME);
        verify(dmsOutPort, times(1)).getProcedureName(eq(FILENAME_DMS_TARGET));
        // setup failure
        when(dmsOutPort.getProcedureName(eq(FILENAME_DMS_TARGET))).thenReturn("asd");
        // call
        final DmsException exception = assertThrows(DmsException.class, () -> processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE));
        assertEquals("Procedure name asd doesn't contain resolved pattern test", exception.getMessage());
    }

    @Test
    void testProcessFile_reuseIncoming() throws UnknownUseCaseException, PresignedUrlException, MetadataException {
        final String useCaseName = "reuseIncoming-incoming";
        // setup
        when(dmsOutPort.getIncomingCooByName(eq(FILENAME_DMS_TARGET), eq(OVERWRITTEN_INCOMING_NAME))).thenReturn(Optional.empty());
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        testDefaults(useCaseName, UseCaseType.PROCEDURE_INCOMING, FILENAME_DMS_TARGET, OVERWRITTEN_INCOMING_NAME, null, FILE_NAME);
        verify(dmsOutPort, times(1)).getIncomingCooByName(eq(FILENAME_DMS_TARGET), eq(OVERWRITTEN_INCOMING_NAME));
        // setup reuse
        when(dmsOutPort.getIncomingCooByName(eq(FILENAME_DMS_TARGET), eq(OVERWRITTEN_INCOMING_NAME))).thenReturn(Optional.of("COO.321.321.321"));
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName, null), FILE);
        // test
        verify(dmsOutPort, times(2)).getIncomingCooByName(eq(FILENAME_DMS_TARGET), eq(OVERWRITTEN_INCOMING_NAME));
        verify(dmsOutPort, times(1)).createProcedureIncoming(any(), any(), any());
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        final DmsTarget dmsTarget = new DmsTarget("COO.321.321.321", useCase.getContext());
        verify(dmsOutPort, times(1)).createContentObject(eq(dmsTarget), eq(new DmsContentObjectRequest(FILE_NAME, null)), eq(null));
    }

    @Test
    void testResolveIncomingParameters_emptyPatternResult() throws MetadataException {
        // setup
        final UseCase useCase = new UseCase();
        final UseCaseIncoming useCaseIncoming = new UseCaseIncoming();
        useCaseIncoming.setIncomingNamePattern("s/^(.+)-(.*)$/${2}/");
        useCase.setIncoming(useCaseIncoming);
        final File file = new File(BUCKET, "test-asd.txt");
        final File fileEmpty = new File(BUCKET, "test-.txt");
        final Metadata metadata = new Metadata(null, Map.of());
        // call
        final DmsIncomingRequest response = processFileUseCase.resolveIncomingParameters(file, useCase, metadata);
        final DmsIncomingRequest responseEmpty = processFileUseCase.resolveIncomingParameters(fileEmpty, useCase, metadata);
        // test
        assertEquals("asd", response.name());
        assertEquals("test-", responseEmpty.name());
    }

    private void testDefaults(final String useCaseName, final UseCaseType targetType, final DmsTarget dmsTarget, final String incomingName,
            final String incomingSubject,
            final String contentObjectName)
            throws UnknownUseCaseException, MetadataException {
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        verify(swimDmsProperties, times(2)).findUseCase(eq(useCaseName));
        verify(targetResolverHelper, times(1)).resolveTargetCoo(eq(targetType), any(), eq(useCase), eq(FILE));
        verify(dmsMetadataHelper, times(0)).resolveInboxDmsTarget(any());
        verify(dmsOutPort, times(0)).createContentObjectInInbox(any(), any(), any());
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(incomingName, incomingSubject, new DmsContentObjectRequest(contentObjectName, null));
        verify(dmsOutPort, times(1)).createProcedureIncoming(eq(dmsTarget), eq(incomingRequest), eq(null));
    }

    private FileEvent buildFileEvent(final String useCaseName, final String metadataPresignedUrl) {
        return new FileEvent(useCaseName, FILE_PRESIGNED_URL, metadataPresignedUrl);
    }
}

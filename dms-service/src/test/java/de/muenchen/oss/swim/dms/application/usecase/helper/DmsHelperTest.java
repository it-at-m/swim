package de.muenchen.oss.swim.dms.application.usecase.helper;

import static de.muenchen.oss.swim.dms.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dms.TestConstants.DUMMY_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsProcedureRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.LoadedFile;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = { SwimDmsProperties.class, PatternHelper.class, RequestResolverHelper.class, DmsHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@ExtendWith(MockitoExtension.class)
class DmsHelperTest {
    private static final DateTimeFormatter SHADOW_PROCEDURE_NAME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter SHADOW_INCOMING_NAME_PATTERN = DateTimeFormatter.ofPattern("dd");
    private static final String DOCUMENT = "document";
    private static final String DOCUMENT_PDF = "document.pdf";
    private static final DmsTarget DMS_TARGET = new DmsTarget("COO.target", "user", "joboe", "jobposition");
    private static final String COO_INCOMING = "COO.incoming";
    private static final String COO_PROCEDURE = "COO.procedure";
    private static final String DOCUMENT_1_FILENAME = "document-1.pdf";
    private static final String DOCUMENT_2_FILENAME = "document-2.pdf";

    @MockitoBean
    private DmsOutPort dmsOutPort;
    @Autowired
    private DmsHelper dmsHelper;

    @Test
    void testProcessInboxContentObject_createsSingleContentObject() {
        final UseCase useCase = new UseCase();
        final LoadedFile loadedFile = loadedFile(DOCUMENT_PDF, DUMMY_STREAM);

        dmsHelper.processInboxContentObject(useCase, DMS_TARGET, List.of(loadedFile));

        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM);
        verify(dmsOutPort).createInboxContentObject(eq(DMS_TARGET), eq(contentObjectRequest));
    }

    @Test
    void testProcessInboxContentObject_rejectsMultipleFiles() {
        final UseCase useCase = new UseCase();
        final List<LoadedFile> files = List.of(
                loadedFile(DOCUMENT_1_FILENAME, DUMMY_STREAM),
                loadedFile(DOCUMENT_2_FILENAME, DUMMY_STREAM));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dmsHelper.processInboxContentObject(useCase, DMS_TARGET, files));

        assertEquals("InboxContentObject can only be created with a single file", exception.getMessage());
        verifyNoInteractions(dmsOutPort);
    }

    @Test
    void testProcessInboxIncoming_createsIncomingWithContentObjects() throws MetadataException {
        final UseCase useCase = new UseCase();
        final List<LoadedFile> files = List.of(
                loadedFile(DOCUMENT_1_FILENAME, DUMMY_STREAM),
                loadedFile(DOCUMENT_2_FILENAME, DUMMY_STREAM));

        dmsHelper.processInboxIncoming(useCase, DMS_TARGET, files);

        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest("document-1", null);
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(
                new DmsContentObjectRequest(DOCUMENT_1_FILENAME, null, DUMMY_STREAM),
                new DmsContentObjectRequest(DOCUMENT_2_FILENAME, null, DUMMY_STREAM));
        verify(dmsOutPort).createInboxIncoming(eq(DMS_TARGET), eq(incomingRequest), eq(contentObjectRequests));
    }

    @Test
    void testProcessProcedureIncoming_createsIncomingWhenReuseDisabled() throws MetadataException {
        final UseCase useCase = new UseCase();
        final LoadedFile loadedFile = loadedFile(DOCUMENT_PDF, DUMMY_STREAM);

        dmsHelper.processProcedureIncoming(useCase, DMS_TARGET, List.of(loadedFile));

        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(DOCUMENT, null);
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM));
        verify(dmsOutPort).createProcedureIncoming(eq(DMS_TARGET), eq(incomingRequest), eq(contentObjectRequests));
        verify(dmsOutPort, never()).getIncomingCooByNamePrefix(eq(DMS_TARGET), eq(DOCUMENT));
    }

    @Test
    void testProcessProcedureIncoming_reusesExistingIncoming() throws MetadataException {
        final UseCase useCase = new UseCase();
        useCase.getIncoming().setReuseIncoming(true);
        final LoadedFile loadedFile = loadedFile(DOCUMENT_PDF, DUMMY_STREAM);
        when(dmsOutPort.getIncomingCooByNamePrefix(eq(DMS_TARGET), eq(DOCUMENT))).thenReturn(Optional.of(COO_INCOMING));

        dmsHelper.processProcedureIncoming(useCase, DMS_TARGET, List.of(loadedFile));

        final DmsTarget incomingDmsTarget = new DmsTarget(COO_INCOMING, DMS_TARGET.getUsername(), DMS_TARGET.getJoboe(), DMS_TARGET.getJobposition());
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM));
        verify(dmsOutPort).addContentObjectsToIncoming(eq(incomingDmsTarget), eq(contentObjectRequests));
        verify(dmsOutPort, never()).createProcedureIncoming(eq(DMS_TARGET), eq(new DmsIncomingRequest(DOCUMENT, null)), eq(contentObjectRequests));
    }

    @Test
    void testProcessProcedureIncoming_verifiesProcedureNameCaseInsensitive() throws MetadataException {
        final UseCase useCase = new UseCase();
        useCase.getIncoming().setVerifyProcedureNamePattern("s/^(.+)-case$/${1}/");
        final LoadedFile loadedFile = loadedFile("MiXeD-case.pdf", DUMMY_STREAM);
        when(dmsOutPort.getProcedureName(eq(DMS_TARGET))).thenReturn("Procedure for mixed value");

        dmsHelper.processProcedureIncoming(useCase, DMS_TARGET, List.of(loadedFile));

        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest("MiXeD-case", null);
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(new DmsContentObjectRequest("MiXeD-case.pdf", null, DUMMY_STREAM));
        verify(dmsOutPort).createProcedureIncoming(eq(DMS_TARGET), eq(incomingRequest), eq(contentObjectRequests));
    }

    @Test
    void testProcessProcedureIncoming_throwsWhenProcedureNameDoesNotMatch() {
        final UseCase useCase = new UseCase();
        useCase.getIncoming().setVerifyProcedureNamePattern("s/^(.+)-case$/${1}/");
        final LoadedFile loadedFile = loadedFile("expected-case.pdf", DUMMY_STREAM);
        when(dmsOutPort.getProcedureName(eq(DMS_TARGET))).thenReturn("different procedure");

        final DmsException exception = assertThrows(DmsException.class,
                () -> dmsHelper.processProcedureIncoming(useCase, DMS_TARGET, List.of(loadedFile)));

        assertEquals("Procedure name different procedure doesn't contain resolved pattern expected", exception.getMessage());
    }

    @Test
    void testProcessShadowFile_createsMissingProcedureAndIncoming() {
        // setup
        final LoadedFile loadedFile = loadedFile(DOCUMENT_PDF, DUMMY_STREAM);
        final String procedureName = LocalDate.now().format(SHADOW_PROCEDURE_NAME_PATTERN);
        final String incomingName = LocalDate.now().format(SHADOW_INCOMING_NAME_PATTERN);
        when(dmsOutPort.getProcedureCooByName(eq(DMS_TARGET), eq(procedureName))).thenReturn(Optional.empty());
        when(dmsOutPort.createFileProcedure(eq(DMS_TARGET), eq(new DmsProcedureRequest(procedureName)))).thenReturn(COO_PROCEDURE);
        // call
        dmsHelper.processShadowFile(DMS_TARGET, List.of(loadedFile));
        // test
        final DmsTarget procedureDmsTarget = new DmsTarget(COO_PROCEDURE, DMS_TARGET);
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(incomingName, null);
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM));
        verify(dmsOutPort).getIncomingCooByNamePrefix(eq(procedureDmsTarget), eq(incomingName));
        verify(dmsOutPort).createProcedureIncoming(eq(procedureDmsTarget), eq(incomingRequest), eq(contentObjectRequests));
    }

    @Test
    void testProcessShadowFile_reusesProcedureAndIncoming() {
        // setup
        final LoadedFile loadedFile = loadedFile(DOCUMENT_PDF, DUMMY_STREAM);
        final String procedureName = LocalDate.now().format(SHADOW_PROCEDURE_NAME_PATTERN);
        final String incomingName = LocalDate.now().format(SHADOW_INCOMING_NAME_PATTERN);
        final DmsTarget procedureDmsTarget = new DmsTarget(COO_PROCEDURE, DMS_TARGET);
        when(dmsOutPort.getProcedureCooByName(eq(DMS_TARGET), eq(procedureName))).thenReturn(Optional.of(COO_PROCEDURE));
        when(dmsOutPort.getIncomingCooByNamePrefix(eq(procedureDmsTarget), eq(incomingName))).thenReturn(Optional.of(COO_INCOMING));
        // call
        dmsHelper.processShadowFile(DMS_TARGET, List.of(loadedFile));
        // test
        final DmsTarget incomingDmsTarget = new DmsTarget(COO_INCOMING, DMS_TARGET.getUsername(), DMS_TARGET.getJoboe(), DMS_TARGET.getJobposition());
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM));
        verify(dmsOutPort, never()).createFileProcedure(eq(DMS_TARGET), eq(new DmsProcedureRequest(procedureName)));
        verify(dmsOutPort).addContentObjectsToIncoming(eq(incomingDmsTarget), eq(contentObjectRequests));
        verify(dmsOutPort, never()).createProcedureIncoming(eq(procedureDmsTarget), eq(new DmsIncomingRequest(incomingName, null)), eq(contentObjectRequests));
    }

    @Test
    void testProcessShadowFile_rejectsMultipleFiles() {
        // setup
        final List<LoadedFile> files = List.of(
                loadedFile(DOCUMENT_1_FILENAME, DUMMY_STREAM),
                loadedFile(DOCUMENT_2_FILENAME, DUMMY_STREAM));
        // call
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dmsHelper.processShadowFile(DMS_TARGET, files));
        // test
        assertEquals("Shadow file can only be created with a single file", exception.getMessage());
        verifyNoInteractions(dmsOutPort);
    }

    private static LoadedFile loadedFile(final String fileName, final InputStream content) {
        return new LoadedFile(new FileReference(BUCKET, fileName), content, new Metadata(null, Map.of()));
    }

}

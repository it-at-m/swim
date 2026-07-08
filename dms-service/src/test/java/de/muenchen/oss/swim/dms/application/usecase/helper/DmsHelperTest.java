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
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.LoadedFile;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.io.InputStream;
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
    private static final String DOCUMENT = "document";
    private static final String DOCUMENT_PDF = "document.pdf";
    private static final DmsTarget DMS_TARGET = new DmsTarget("COO.target", "user", "joboe", "jobposition");

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
                loadedFile("document-1.pdf", DUMMY_STREAM),
                loadedFile("document-2.pdf", DUMMY_STREAM));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dmsHelper.processInboxContentObject(useCase, DMS_TARGET, files));

        assertEquals("InboxContentObject can only be created with a single file", exception.getMessage());
        verifyNoInteractions(dmsOutPort);
    }

    @Test
    void testProcessInboxIncoming_createsIncomingWithContentObjects() throws MetadataException {
        final UseCase useCase = new UseCase();
        final List<LoadedFile> files = List.of(
                loadedFile("document-1.pdf", DUMMY_STREAM),
                loadedFile("document-2.pdf", DUMMY_STREAM));

        dmsHelper.processInboxIncoming(useCase, DMS_TARGET, files);

        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest("document-1", null);
        final List<DmsContentObjectRequest> contentObjectRequests = List.of(
                new DmsContentObjectRequest("document-1.pdf", null, DUMMY_STREAM),
                new DmsContentObjectRequest("document-2.pdf", null, DUMMY_STREAM));
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
        when(dmsOutPort.getIncomingCooByNamePrefix(eq(DMS_TARGET), eq(DOCUMENT))).thenReturn(Optional.of("COO.incoming"));

        dmsHelper.processProcedureIncoming(useCase, DMS_TARGET, List.of(loadedFile));

        final DmsTarget incomingDmsTarget = new DmsTarget("COO.incoming", DMS_TARGET.getUsername(), DMS_TARGET.getJoboe(), DMS_TARGET.getJobposition());
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

    private static LoadedFile loadedFile(final String fileName, final InputStream content) {
        return new LoadedFile(new FileReference(BUCKET, fileName), content, new Metadata(null, Map.of()));
    }

}

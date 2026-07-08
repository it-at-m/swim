package de.muenchen.oss.swim.dms.application.usecase.helper;

import static de.muenchen.oss.swim.dms.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dms.TestConstants.DUMMY_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseContentObject;
import de.muenchen.oss.swim.dms.domain.model.UseCaseIncoming;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = { SwimDmsProperties.class, PatternHelper.class, RequestResolverHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class RequestResolverHelperTest {
    private static final String DOCUMENT_PDF = "document.pdf";

    @Autowired
    private RequestResolverHelper requestResolverHelper;

    @Test
    void testResolveContentObjectParameters_usesOriginalFilenameWhenNoPattern() {
        final UseCase useCase = new UseCase();
        final FileReference file = new FileReference(BUCKET, "folder/source-file.pdf");

        final DmsContentObjectRequest response = requestResolverHelper.resolveContentObjectParameters(file, useCase, null, DUMMY_STREAM);

        assertEquals("source-file.pdf", response.name());
        assertNull(response.subject());
        assertSame(DUMMY_STREAM, response.inputStream());
    }

    @Test
    void testResolveContentObjectParameters_appliesFilenameOverwritePattern() {
        final UseCase useCase = new UseCase();
        final UseCaseContentObject contentObject = new UseCaseContentObject();
        contentObject.setFilenameOverwritePattern("s/^(.+)-(.+)$/${2}-${1}/");
        useCase.setContentObject(contentObject);
        final FileReference file = new FileReference(BUCKET, "first-second.txt");

        final DmsContentObjectRequest response = requestResolverHelper.resolveContentObjectParameters(file, useCase, null, DUMMY_STREAM);

        assertEquals("second-first.txt", response.name());
        assertNull(response.subject());
    }

    @Test
    void testResolveContentObjectParameters_appliesSubjectPattern() {
        final UseCase useCase = new UseCase();
        final UseCaseContentObject contentObject = new UseCaseContentObject();
        contentObject.setSubjectPattern("s/^(.+)-(.+)$/${1}/");
        useCase.setContentObject(contentObject);
        final FileReference file = new FileReference(BUCKET, "subject-rest.pdf");

        final DmsContentObjectRequest response = requestResolverHelper.resolveContentObjectParameters(file, useCase, null, DUMMY_STREAM);

        assertEquals("subject-rest.pdf", response.name());
        assertEquals("subject", response.subject());
    }

    @Test
    void testResolveIncomingParameters_usesContentObjectNameWithoutExtensionWhenNoPattern() throws MetadataException {
        final UseCase useCase = new UseCase();
        final FileReference file = new FileReference(BUCKET, "original-file.pdf");
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest("overwritten-name.pdf", null, DUMMY_STREAM);

        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, null, contentObjectRequest);

        assertEquals("overwritten-name", response.name());
        assertNull(response.subject());
    }

    @Test
    void testResolveIncomingParameters_appliesIncomingNamePattern() throws MetadataException {
        final UseCase useCase = new UseCase();
        final UseCaseIncoming incoming = new UseCaseIncoming();
        incoming.setIncomingNamePattern("s/^(.+)-(.+)$/${2}/");
        useCase.setIncoming(incoming);
        final FileReference file = new FileReference(BUCKET, "prefix-target.pdf");
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest("fallback.pdf", null, DUMMY_STREAM);

        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, null, contentObjectRequest);

        assertEquals("target", response.name());
        assertNull(response.subject());
    }

    @Test
    void testResolveIncomingParameters_appliesIncomingSubjectPattern() throws MetadataException {
        final UseCase useCase = new UseCase();
        final UseCaseIncoming incoming = new UseCaseIncoming();
        incoming.setIncomingSubjectPattern("s/^(.+)-(.+)$/${1}/");
        incoming.setMetadataSubject(true);
        useCase.setIncoming(incoming);
        final FileReference file = new FileReference(BUCKET, "subject-rest.pdf");
        final Metadata metadata = new Metadata(null, Map.of("FdE_Key", "metadata subject"));
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest("fallback.pdf", null, DUMMY_STREAM);

        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjectRequest);

        assertEquals("fallback", response.name());
        assertEquals("subject", response.subject());
    }

    @Test
    void testResolveIncomingParameters_buildsSubjectFromMetadata() throws MetadataException {
        final UseCase useCase = new UseCase();
        final UseCaseIncoming incoming = new UseCaseIncoming();
        incoming.setMetadataSubject(true);
        useCase.setIncoming(incoming);
        final FileReference file = new FileReference(BUCKET, DOCUMENT_PDF);
        final Metadata metadata = new Metadata(null, Map.of(
                "FdE_Key_2", "Value 2",
                "Ignored", "Ignored value",
                "FdE_Key_1", "Value 1"));
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM);

        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjectRequest);

        assertEquals("document", response.name());
        assertEquals("Value 1 (Key_1)\nValue 2 (Key_2)", response.subject());
    }

    @Test
    void testResolveIncomingParameters_requiresMetadataForMetadataSubject() {
        final UseCase useCase = new UseCase();
        final UseCaseIncoming incoming = new UseCaseIncoming();
        incoming.setMetadataSubject(true);
        useCase.setIncoming(incoming);
        final FileReference file = new FileReference(BUCKET, DOCUMENT_PDF);
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(DOCUMENT_PDF, null, DUMMY_STREAM);

        final MetadataException exception = assertThrows(MetadataException.class,
                () -> requestResolverHelper.resolveIncomingParameters(file, useCase, null, contentObjectRequest));

        assertEquals("Metadata is required", exception.getMessage());
    }

    @Test
    void testResolveIncomingParameters_emptyPatternResult() throws MetadataException {
        // setup
        final UseCase useCase = new UseCase();
        final UseCaseIncoming useCaseIncoming = new UseCaseIncoming();
        useCaseIncoming.setIncomingNamePattern("s/^(.+)-(.*)$/${2}/");
        useCase.setIncoming(useCaseIncoming);
        final FileReference file = new FileReference(BUCKET, "test-asd.txt");
        final FileReference fileEmpty = new FileReference(BUCKET, "test-.txt");
        final Metadata metadata = new Metadata(null, Map.of());
        // call
        final DmsContentObjectRequest contentObjectRequest = requestResolverHelper.resolveContentObjectParameters(file, useCase, metadata, DUMMY_STREAM);
        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjectRequest);
        final DmsContentObjectRequest contentObjectRequestEmpty = requestResolverHelper.resolveContentObjectParameters(fileEmpty, useCase, metadata,
                DUMMY_STREAM);
        final DmsIncomingRequest responseEmpty = requestResolverHelper.resolveIncomingParameters(fileEmpty, useCase, metadata, contentObjectRequestEmpty);
        // test
        assertEquals("asd", response.name());
        assertEquals("test-", responseEmpty.name());
    }

    @Test
    void testResolveIncomingParameters_usesResolvedContentObjectNameWhenPatternResultIsBlank() throws MetadataException {
        // setup
        final UseCase useCase = new UseCase();
        final UseCaseIncoming incoming = new UseCaseIncoming();
        incoming.setIncomingNamePattern("s/^(.+)-(.*)$/${2}/");
        useCase.setIncoming(incoming);
        final FileReference file = new FileReference(BUCKET, "original-.pdf");
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest("resolved-name.pdf", null, DUMMY_STREAM);
        // call
        final DmsIncomingRequest response = requestResolverHelper.resolveIncomingParameters(file, useCase, null, contentObjectRequest);
        // test
        assertEquals("resolved-name", response.name());
    }

}

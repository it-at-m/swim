package de.muenchen.oss.swim.dipa.application.usecase;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dipa.TestConstants;
import de.muenchen.oss.swim.dipa.application.port.out.DipaOutPort;
import de.muenchen.oss.swim.dipa.configuration.DipaMeter;
import de.muenchen.oss.swim.dipa.configuration.SwimDipaProperties;
import de.muenchen.oss.swim.dipa.domain.model.DipaRequestContext;
import de.muenchen.oss.swim.dipa.domain.model.dipa.ContentObjectRequest;
import de.muenchen.oss.swim.dipa.domain.model.dipa.HrSubfileContext;
import de.muenchen.oss.swim.dipa.domain.model.dipa.IncomingRequest;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
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
        classes = { SwimDipaProperties.class, ProcessFileUseCase.class, ObjectMapper.class, PatternHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ProcessFileUseCaseTest {
    @MockitoSpyBean
    @Autowired
    private SwimDipaProperties swimDipaProperties;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private DipaOutPort dipaOutPort;
    @MockitoBean
    private FileEventOutPort fileEventOutPort;
    @MockitoBean
    private DipaMeter dipaMeter;
    @MockitoSpyBean
    @Autowired
    private ProcessFileUseCase processFileUseCase;

    private final static String BUCKET = "test-bucket";
    private final static String FILE_NAME_WITHOUT_EXTENSION = "test_asd";
    private final static String FILE_NAME = String.format("%s.pdf", FILE_NAME_WITHOUT_EXTENSION);
    private final static String FILE_PATH = String.format("test-path/%s", FILE_NAME);
    private final static File FILE = new File(BUCKET, FILE_PATH);
    private final static String FILE_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, FILE_PATH);
    private final static DipaRequestContext REQUEST_CONTEXT = new DipaRequestContext("staticUsername");

    @BeforeEach
    void setup() throws PresignedUrlException {
        when(fileSystemOutPort.getPresignedUrlFile(eq(FILE_PRESIGNED_URL))).thenReturn(null);
    }

    @Test
    void testProcessFile_HrSubfileStatic() throws UnknownUseCaseException, PresignedUrlException {
        final String useCaseName = "hr_subfile_incoming-static";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName), FILE);
        // test
        verify(swimDipaProperties, times(1)).findUseCase(eq(useCaseName));
        final HrSubfileContext context = new HrSubfileContext(REQUEST_CONTEXT, "staticPersNr", "staticCategory");
        final ContentObjectRequest contentObject = new ContentObjectRequest(FILE_NAME_WITHOUT_EXTENSION, "pdf", null);
        final IncomingRequest request = new IncomingRequest(null, contentObject);
        verify(dipaOutPort, times(1)).createHrSubfileIncoming(eq(context), eq(request));
        verify(dipaMeter, times(1)).incrementProcessed(eq(useCaseName), eq("HR_SUBFILE_INCOMING"));
    }

    @Test
    void testProcessFile_HrSubfileFilename() throws UnknownUseCaseException, PresignedUrlException {
        final String useCaseName = "hr_subfile_incoming-filename";
        // call
        processFileUseCase.processFile(buildFileEvent(useCaseName), FILE);
        // test
        verify(swimDipaProperties, times(1)).findUseCase(eq(useCaseName));
        final HrSubfileContext context = new HrSubfileContext(REQUEST_CONTEXT, "test", "asd");
        final ContentObjectRequest contentObject = new ContentObjectRequest("asd", "pdf", null);
        final IncomingRequest request = new IncomingRequest("test", contentObject);
        verify(dipaOutPort, times(1)).createHrSubfileIncoming(eq(context), eq(request));
        verify(dipaMeter, times(1)).incrementProcessed(eq(useCaseName), eq("HR_SUBFILE_INCOMING"));
    }

    private FileEvent buildFileEvent(final String useCaseName) {
        return new FileEvent(useCaseName, FILE_PRESIGNED_URL, null);
    }
}

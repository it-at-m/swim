package de.muenchen.oss.swim.dms.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.File;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { SwimDmsProperties.class, ProcessFileUseCase.class, ObjectMapper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class ProcessFileUseCaseTest {
    @Autowired
    private SwimDmsProperties swimDmsProperties;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private DmsOutPort dmsOutPort;
    @MockitoBean
    private FileEventOutPort fileEventOutPort;
    @MockitoSpyBean
    @Autowired
    private ProcessFileUseCase processFileUseCase;

    private final static String BUCKET = "test-bucket";
    private final static String FILE_PATH = "test-path/test.pdf";
    private final static File FILE = new File(BUCKET, FILE_PATH);
    private final static String FILE_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, FILE_PATH);
    private final static String METADAT_PATH = "test-path/test.json";
    private final static String METADATA_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, METADAT_PATH);
    private final static DmsTarget METADATA_DMS_TARGET = new DmsTarget("metadataCoo", "metadata.username", null, null);
    private final static DmsTarget STATIC_DMS_TARGET = new DmsTarget("staticCoo", "staticUsername", "staticJobOe", "staticJobPosition");

    @Test
    void testProcessFile_MetadataInbox() {
        final String useCaseName = "metadata-inbox";
        final UseCase useCase = processFileUseCase.findUseCase(useCaseName);
        // setup
        when(fileSystemOutPort.getPresignedUrlFile(eq(FILE_PRESIGNED_URL))).thenReturn(null);
        when(fileSystemOutPort.getPresignedUrlFile(eq(METADATA_PRESIGNED_URL))).thenReturn(getClass().getResourceAsStream("/files/example-metadata.json"));
        // call
        processFileUseCase.processFile(useCaseName, FILE, FILE_PRESIGNED_URL, METADATA_PRESIGNED_URL);
        // test
        verify(processFileUseCase, times(2)).findUseCase(eq(useCaseName));
        verify(processFileUseCase, times(1)).resolveTargetCoo(eq(METADATA_PRESIGNED_URL), eq(useCase), eq(FILE));
        verify(processFileUseCase, times(1)).extractCooFromMetadata(any());
        verify(dmsOutPort, times(1)).putFileInInbox(eq(METADATA_DMS_TARGET), eq("test.pdf"), eq(null));
    }

    @Test
    void testProcessFile_StaticIncoming() {
        final String useCaseName = "static-incoming";
        final UseCase useCase = processFileUseCase.findUseCase(useCaseName);
        // setup
        when(fileSystemOutPort.getPresignedUrlFile(eq(FILE_PRESIGNED_URL))).thenReturn(null);
        // call
        processFileUseCase.processFile(useCaseName, FILE, FILE_PRESIGNED_URL, null);
        // test
        verify(processFileUseCase, times(2)).findUseCase(eq(useCaseName));
        verify(processFileUseCase, times(1)).resolveTargetCoo(isNull(), eq(useCase), eq(FILE));
        verify(processFileUseCase, times(0)).extractCooFromMetadata(any());
        verify(dmsOutPort, times(0)).putFileInInbox(any(), any(), any());
        verify(dmsOutPort, times(1)).createIncoming(eq(STATIC_DMS_TARGET), eq("test.pdf"), eq("test.pdf"), eq(null));
    }

    @Test
    void testFindUseCase() {
        final String useCaseName = "metadata-inbox";
        // call
        final UseCase useCase = processFileUseCase.findUseCase(useCaseName);
        // test
        assertEquals(useCaseName, useCase.getName());
    }

    @Test
    void testExtractMetadataCoo() {
        // call
        final DmsTarget dmsTarget = processFileUseCase.extractCooFromMetadata(getClass().getResourceAsStream("/files/example-metadata.json"));
        // test
        assertEquals(METADATA_DMS_TARGET, dmsTarget);
    }
}
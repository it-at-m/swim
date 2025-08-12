package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TENANT;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL;
import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_PRESIGNED_URL_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { S3Adapter.class, S3Properties.class, SwimDispatcherProperties.class, ProtocolMapperImpl.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class S3AdapterTest {
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;
    @Autowired
    private S3Adapter s3Adapter;

    @Test
    void fileFromPresignedUrl_Success() throws PresignedUrlException {
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        final File file = s3Adapter.fileFromPresignedUrl(useCase, TEST_PRESIGNED_URL);
        assertEquals(TENANT, file.tenant());
        assertEquals(BUCKET, file.bucket());
        assertEquals(TEST_PRESIGNED_URL_PATH, file.path());
    }

    @Test
    void fileFromPresignedUrl_Exception() {
        final UseCase useCase = swimDispatcherProperties.getUseCases().get(1);
        assertThrows(PresignedUrlException.class, () -> s3Adapter.fileFromPresignedUrl(useCase, TEST_PRESIGNED_URL));
    }
}

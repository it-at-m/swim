package de.muenchen.oss.swim.dms.application.usecase.helper;

import static de.muenchen.oss.swim.dms.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dms.TestConstants.DUMMY_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
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
    @Autowired
    private RequestResolverHelper requestResolverHelper;

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

}

package de.muenchen.oss.swim.dms.application.usecase.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = { SwimDmsProperties.class, TargetResolverHelper.class, ObjectMapper.class, DmsMetadataHelper.class, PatternHelper.class }
)
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class TargetResolverHelperTest {
    @Autowired
    private TargetResolverHelper targetResolverHelper;
    @MockitoBean
    private DmsOutPort dmsOutPort;

    @Test
    void testResolveTypeFromMetadataFile() throws MetadataException {
        final Metadata metadata = new Metadata(null, Map.of("SWIM_DMS_Target", "inbox_incoming"));
        assertEquals(UseCaseType.INBOX_INCOMING, targetResolverHelper.resolveTypeFromMetadataFile(metadata));
    }

    @Test
    void testResolveTypeFromMetadataFile_KeyMissing() {
        // empty map
        final Metadata metadata = new Metadata(null, Map.of());
        final MetadataException exception = assertThrows(MetadataException.class, () -> targetResolverHelper.resolveTypeFromMetadataFile(metadata));
        assertEquals("DMS target key 'SWIM_DMS_Target' not found in metadata file or empty", exception.getMessage());
        // empty value
        final Metadata metadata2 = new Metadata(null, Map.of("SWIM_DMS_Target", ""));
        final MetadataException exception2 = assertThrows(MetadataException.class, () -> targetResolverHelper.resolveTypeFromMetadataFile(metadata2));
        assertEquals("DMS target key 'SWIM_DMS_Target' not found in metadata file or empty", exception2.getMessage());
    }
}

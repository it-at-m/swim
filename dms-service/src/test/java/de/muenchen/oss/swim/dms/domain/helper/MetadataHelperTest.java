package de.muenchen.oss.swim.dms.domain.helper;

import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_GROUP;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDmsProperties.class, MetadataHelper.class, ObjectMapper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class MetadataHelperTest {
    @Autowired
    private MetadataHelper metadataHelper;

    @Test
    void testResolveDmsTarget() throws MetadataException {
        // test user
        final DmsTarget dmsTargetUser = metadataHelper.resolveDmsTarget(getClass().getResourceAsStream("/files/example-metadata-user.json"));
        assertEquals(METADATA_DMS_TARGET_USER, dmsTargetUser);
        // test group
        final DmsTarget dmsTargetGroup = metadataHelper.resolveDmsTarget(getClass().getResourceAsStream("/files/example-metadata-group.json"));
        assertEquals(METADATA_DMS_TARGET_GROUP, dmsTargetGroup);
        // test invalid both
        assertThrows(MetadataException.class,
                () -> metadataHelper.resolveDmsTarget(getClass().getResourceAsStream("/files/example-metadata-invalid-both.json")));
        // test invalid none
        assertThrows(MetadataException.class,
                () -> metadataHelper.resolveDmsTarget(getClass().getResourceAsStream("/files/example-metadata-invalid-none.json")));
    }
}

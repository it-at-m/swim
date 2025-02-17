package de.muenchen.oss.swim.dms.domain.helper;

import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_GROUP;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
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
    void testResolveInboxDmsTarget() throws MetadataException {
        // test user
        final JsonNode metadataUserNode = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-user.json"));
        final DmsTarget dmsTargetUser = metadataHelper.resolveInboxDmsTarget(metadataUserNode);
        assertEquals(METADATA_DMS_TARGET_USER, dmsTargetUser);
        // test group
        final JsonNode metadataGroupNode = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-group.json"));
        final DmsTarget dmsTargetGroup = metadataHelper.resolveInboxDmsTarget(metadataGroupNode);
        assertEquals(METADATA_DMS_TARGET_GROUP, dmsTargetGroup);
        // test invalid both
        final JsonNode metadataInvalidBoth = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-both.json"));
        assertThrows(MetadataException.class,
                () -> metadataHelper.resolveInboxDmsTarget(metadataInvalidBoth));
        // test invalid none
        final JsonNode metadataInvalidNone = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-none.json"));
        assertThrows(MetadataException.class,
                () -> metadataHelper.resolveInboxDmsTarget(metadataInvalidNone));
        // test invalid empty
        final JsonNode metadataInvalidEmpty = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-empty.json"));
        assertThrows(MetadataException.class,
                () -> metadataHelper.resolveInboxDmsTarget(metadataInvalidEmpty));
    }
}

package de.muenchen.oss.swim.dms.domain.helper;

import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_GROUP;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_INCOMING;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_OU_WORK_QUEUE;
import static de.muenchen.oss.swim.dms.TestConstants.METADATA_DMS_TARGET_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDmsProperties.class, DmsMetadataHelper.class, ObjectMapper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class DmsMetadataHelperTest {
    @Autowired
    private DmsMetadataHelper dmsMetadataHelper;

    @Test
    void testResolveInboxDmsTarget() throws MetadataException {
        // test user
        final Metadata metadataUser = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-user.json"));
        final DmsTarget dmsTargetUser = dmsMetadataHelper.resolveInboxDmsTarget(metadataUser);
        assertEquals(METADATA_DMS_TARGET_USER, dmsTargetUser);
        // test group
        final Metadata metadataGroup = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-group.json"));
        final DmsTarget dmsTargetGroup = dmsMetadataHelper.resolveInboxDmsTarget(metadataGroup);
        assertEquals(METADATA_DMS_TARGET_GROUP, dmsTargetGroup);
        // test invalid both
        final Metadata metadataInvalidBoth = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-both.json"));
        assertThrows(MetadataException.class,
                () -> dmsMetadataHelper.resolveInboxDmsTarget(metadataInvalidBoth));
        // test invalid none
        final Metadata metadataInvalidNone = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-none.json"));
        assertThrows(MetadataException.class,
                () -> dmsMetadataHelper.resolveInboxDmsTarget(metadataInvalidNone));
        // test invalid empty
        final Metadata metadataInvalidEmpty = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-invalid-empty.json"));
        assertThrows(MetadataException.class,
                () -> dmsMetadataHelper.resolveInboxDmsTarget(metadataInvalidEmpty));
    }

    @Test
    void testResolveIncomingDmsTarget() throws MetadataException {
        // test inbox
        final Metadata metadata = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-incoming.json"));
        final DmsTarget dmsTarget = dmsMetadataHelper.resolveIncomingDmsTarget(metadata);
        assertEquals(METADATA_DMS_TARGET_INCOMING, dmsTarget);
        // test ou work queue
        final Metadata metadataNodeWorkQueue = dmsMetadataHelper
                .parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-ou-work-queue.json"));
        final DmsTarget dmsTargetWorkQueue = dmsMetadataHelper.resolveIncomingDmsTarget(metadataNodeWorkQueue);
        assertEquals(METADATA_DMS_TARGET_OU_WORK_QUEUE, dmsTargetWorkQueue);
    }
}

package de.muenchen.oss.swim.dms;

import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";

    public static final String BUCKET = "test-bucket";
    public static final InputStream DUMMY_STREAM = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    public static final DmsTarget METADATA_DMS_TARGET_USER = new DmsTarget("userMetadataCoo", "metadata.user", null, null);
    public static final DmsTarget METADATA_DMS_TARGET_GROUP = new DmsTarget("groupMetadataCoo", "metadata.group", null, null);
    public static final DmsTarget METADATA_DMS_TARGET_INCOMING = new DmsTarget("incomingMetadataCoo", "metadata.user", null, null);
    public static final DmsTarget METADATA_DMS_TARGET_OU_WORK_QUEUE = new DmsTarget(null, "metadata.user", null, null);

}

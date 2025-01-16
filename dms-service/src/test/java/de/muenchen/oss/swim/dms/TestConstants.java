package de.muenchen.oss.swim.dms;

import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";
    public static final String SPRING_NO_SECURITY_PROFILE = "no-security";
    public static final String SPRING_JSON_LOGGING_PROFILE = "json-logging";

    public final static DmsTarget METADATA_DMS_TARGET_USER = new DmsTarget("userMetadataCoo", "metadata.user", null, null);
    public final static DmsTarget METADATA_DMS_TARGET_GROUP = new DmsTarget("groupMetadataCoo", "metadata.group", null, null);

}

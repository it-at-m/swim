package de.muenchen.oss.swim.invoice;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";
    public static final String SPRING_NO_SECURITY_PROFILE = "no-security";
    public static final String SPRING_JSON_LOGGING_PROFILE = "json-logging";

}

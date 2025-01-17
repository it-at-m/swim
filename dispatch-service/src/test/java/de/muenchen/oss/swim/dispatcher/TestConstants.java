package de.muenchen.oss.swim.dispatcher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";
    public static final String SPRING_JSON_LOGGING_PROFILE = "json-logging";

    public static final String TEST_USE_CASE = "test-meta";
    public static final String BUCKET = "test-bucket";
    public static final String TEST_PRESIGNED_URL = "https://s3.muenchen.de/test-bucket/test/path/example.pdf";
    public static final String TEST_PRESIGNED_URL_PATH = "test/path/example.pdf";
}

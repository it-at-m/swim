package de.muenchen.oss.swim.dispatcher;

import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.FileEvent;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";
    public static final String SPRING_JSON_LOGGING_PROFILE = "json-logging";

    public static final String TENANT = "test-tenant";
    public static final String BUCKET = "test-bucket";
    public static final String USE_CASE = "test-meta";
    public static final List<String> USE_CASE_RECIPIENTS = List.of("test-meta@example.com");
    public static final String FOLDER_PATH = "test/inProcess/path/";
    public static final File FILE1 = new File(TENANT, BUCKET, "test/inProcess/path/example.pdf", 0L);
    public static final File FILE2 = new File(TENANT, BUCKET, "test/inProcess/path/example2.pdf", 0L);
    public static final Map<String, String> TAGS = Map.of("SWIM_State", "processed");
    public static final String USE_CASE_PATH = "test";
    public static final String USE_CASE_DISPATCH_PATH = USE_CASE_PATH + "/inProcess";
    public static final String TEST_PRESIGNED_URL = "https://s3.muenchen.de/test-bucket/test/inProcess/path/example.pdf";
    public static final String TEST_PRESIGNED_URL_PATH = "test/inProcess/path/example.pdf";
    public static final FileEvent TEST_FILE_EVENT = new FileEvent(
            USE_CASE, TEST_PRESIGNED_URL, null
    );
}

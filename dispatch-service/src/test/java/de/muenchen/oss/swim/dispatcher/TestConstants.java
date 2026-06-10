package de.muenchen.oss.swim.dispatcher;

import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";

    public static final String BUCKET = "test-bucket";
    public static final String USE_CASE = "test-meta";
    public static final List<String> USE_CASE_RECIPIENTS = List.of("test-meta@example.com");
    public static final String FOLDER_PATH = "test/inProcess/path/";
    public static final Map<String, String> TAGS = Map.of("SWIM_State", "processed");
    public static final FileWithMetadata FILE1 = new FileWithMetadata(new FileReference(BUCKET, "test/inProcess/path/test.pdf"), 0L, TAGS);
    public static final FileWithMetadata FILE2 = new FileWithMetadata(new FileReference(BUCKET, "test/inProcess/path/test2.pdf"), 0L, TAGS);
    public static final String USE_CASE_PATH = "test";
    public static final String USE_CASE_DISPATCH_PATH = USE_CASE_PATH + "/inProcess";
    public static final String TEST_PRESIGNED_URL = "https://s3.muenchen.de/test-bucket/test/inProcess/path/example.pdf";
    public static final String TEST_PRESIGNED_URL_PATH = "test/inProcess/path/example.pdf";
    public static final FileReference TEST_PRESIGNED_URL_FILE = new FileReference(BUCKET, "test/inProcess/path/example.pdf");
}

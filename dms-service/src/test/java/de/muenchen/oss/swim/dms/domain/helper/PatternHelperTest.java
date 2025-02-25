package de.muenchen.oss.swim.dms.domain.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { PatternHelper.class, DmsMetadataHelper.class, ObjectMapper.class, SwimDmsProperties.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class PatternHelperTest {
    public static final String TEST_FILENAME = "Test123-COO123.123.123-ExampleTest.pdf";
    @Autowired
    private DmsMetadataHelper dmsMetadataHelper;
    @Autowired
    private PatternHelper patternHelper;

    @Test
    void testApplyPattern() throws MetadataException {
        // null pattern
        final String resultNull = patternHelper.applyPattern(null, "input", null);
        assertEquals("input", resultNull);
        // regex pattern
        final String resultRegex = patternHelper.applyPattern("s/(.+)-COO[\\d\\.]+-(.*)/${1}-${2}/", TEST_FILENAME, null);
        assertEquals("Test123-ExampleTest.pdf", resultRegex);
        // named regex and metadata pattern
        final Metadata metadata = dmsMetadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata-user.json"));
        final String result = patternHelper.applyPattern("s/(?<p1>.+)-COO[\\d\\.]+-(?<p2>.*)/${p1}_${if.PPK_Username}_${p2}/m", TEST_FILENAME, metadata);
        assertEquals("Test123_metadata.user_ExampleTest.pdf", result);
        // missing metadata
        assertThrows(IllegalArgumentException.class, () -> patternHelper.applyPattern("s/(.+)-COO[\\d\\.]+-(.*)/${1}-${2}/m", TEST_FILENAME, null));
    }
}

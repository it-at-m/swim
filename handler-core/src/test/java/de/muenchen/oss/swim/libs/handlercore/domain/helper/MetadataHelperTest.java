package de.muenchen.oss.swim.libs.handlercore.domain.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { MetadataHelper.class, ObjectMapper.class })
class MetadataHelperTest {
    @Autowired
    private MetadataHelper metadataHelper;

    @Test
    void testGetIndexFields() throws MetadataException {
        final JsonNode metadataUserNode = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata.json"));
        final Map<String, String> expected = Map.of(
                "Key1", "Value1",
                "Key2", "Value2"
        );
        assertEquals(expected, metadataHelper.getIndexFields(metadataUserNode));
    }
}
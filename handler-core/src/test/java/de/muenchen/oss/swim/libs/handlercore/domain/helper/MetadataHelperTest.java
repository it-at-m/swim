package de.muenchen.oss.swim.libs.handlercore.domain.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { MetadataHelper.class, ObjectMapper.class })
class MetadataHelperTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MetadataHelper metadataHelper;

    @Test
    void testGetIndexFields() throws MetadataException {
        final Metadata metadata = metadataHelper.parseMetadataFile(getClass().getResourceAsStream("/files/example-metadata.json"));
        final Map<String, String> expected = Map.of(
                "Key1", "Value1",
                "Key2", "Value2");
        assertEquals(expected, metadata.indexFields());
    }

    @Test
    void testGetIndexFields_MissingKeys() {
        // no Document key
        final JsonNode invalidNode = objectMapper.createObjectNode();
        assertThrows(MetadataException.class, () -> metadataHelper.getIndexFields(invalidNode));
        // no IndexFields key
        final ObjectNode documentNode = objectMapper.createObjectNode();
        final ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set(MetadataHelper.METADATA_DOCUMENT_KEY, documentNode);
        assertThrows(MetadataException.class, () -> metadataHelper.getIndexFields(rootNode));
    }
}

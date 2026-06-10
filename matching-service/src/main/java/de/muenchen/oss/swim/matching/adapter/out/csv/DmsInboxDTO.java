package de.muenchen.oss.swim.matching.adapter.out.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DmsInboxDTO(
        @JsonProperty(value = "Externe ID", required = true) String lhmObjectId,
        @JsonProperty(value = "Adresse", required = true) String coo,
        @JsonProperty(value = "Name", required = true) String name,
        @JsonProperty(value = "ACL für neue/importierte Schriftstücke", required = true) @JsonDeserialize(using = InboxTypeDeserializer.class) InboxType type,
        @JsonProperty(value = "Organisationseinheit", required = true) String ou,
        @JsonProperty(value = "Mandant", required = true) String mandant) {

    private static final String ACL_USER = "ACL für persönliche Daten des Eigentümers";
    public static final String ACL_GROUP = "ACL für Schriftgutobjekte der Organisationseinheit";

    public enum InboxType {
        @JsonProperty(ACL_USER) USER,
        @JsonProperty(ACL_GROUP) GROUP
    }

    /**
     * Custom deserializer for {@link InboxType} with default value {@link InboxType#USER}.
     */
    private static final class InboxTypeDeserializer extends JsonDeserializer<InboxType> {
        @Override
        public InboxType deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException {
            final String value = p.getText();
            // default USER
            if (value == null || value.isEmpty()) {
                return InboxType.USER;
            }
            // handle CSV values
            if (ACL_USER.equals(value)) {
                return InboxType.USER;
            } else if (ACL_GROUP.equals(value)) {
                return InboxType.GROUP;
            }
            throw new IOException("Unknown value for InboxType: " + value);
        }
    }
}

package de.muenchen.oss.swim.matching.adapter.out.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DmsInboxDTO(
        @JsonProperty("Externe ID") String lhmObjectId,
        @JsonProperty("Adresse") String coo,
        @JsonProperty("Name") String name,
        @JsonProperty("ACL für neue/importierte Schriftstücke") @JsonDeserialize(using = InboxTypeDeserializer.class) InboxType type,
        @JsonProperty("Organisationseinheit") String ou,
        @JsonProperty("Mandant") String mandant) {

    private static final String ACL_USER = "ACL für persönliche Daten des Eigentümers";
    public static final String ACL_GROUP = "ACL für Schriftgutobjekte der Organisationseinheit";

    public enum InboxType {
        @JsonProperty(ACL_USER) USER,
        @JsonProperty(ACL_GROUP) GROUP
    }

    /**
     * Custom deserializer for {@link InboxType} with default value {@link InboxType#USER}.
     */
    private static final class InboxTypeDeserializer extends ValueDeserializer<InboxType> {
        @Override
        public InboxType deserialize(final JsonParser p, final DeserializationContext ctxt) {
            final String value = p.getString();
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
            throw DatabindException.from(p, "Unknown value for InboxType: " + value);
        }
    }
}

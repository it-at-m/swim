package de.muenchen.oss.swim.matching.adapter.out.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DmsInboxDTO(
        @JsonProperty("Externe ID") String lhmObjectId,
        @JsonProperty("Adresse") String coo,
        @JsonProperty("Name") String name,
        @JsonProperty("ACL für neue/importierte Schriftstücke") InboxType type,
        @JsonProperty("Organisationseinheit") String ou,
        @JsonProperty("Mandant") String mandant) {
    public enum InboxType {
        @JsonProperty("ACL für persönliche Daten des Eigentümers") USER,
        @JsonProperty("ACL für Schriftgutobjekte der Organisationseinheit") GROUP
    }
}

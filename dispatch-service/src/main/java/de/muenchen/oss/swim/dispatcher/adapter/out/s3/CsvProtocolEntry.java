package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class CsvProtocolEntry {
    @JsonProperty(value = "PDF", required = true)
    @JsonAlias("Dateiname")
    private String fileName;
    @JsonProperty(value = "Seiten", required = true)
    private int pageCount;
    @JsonProperty("RefEB")
    private String department;
    @JsonProperty("KistenID")
    private String boxId;
    @JsonProperty("Paginiernummer")
    private String paginationId;
    @JsonProperty("Belegart")
    @JsonAlias({ "Dokumentart", "Dokumentenart", "Dokumenttyp" })
    private String documentType;
    @JsonProperty("COO-Adresse")
    private String cooAddress;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    protected void setAdditionalProperty(final String key, final Object value) {
        this.additionalProperties.put(key, value);
    }
}

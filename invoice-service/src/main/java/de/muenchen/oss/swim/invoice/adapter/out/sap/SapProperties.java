package de.muenchen.oss.swim.invoice.adapter.out.sap;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Data
@ConfigurationProperties("swim.sap")
@Validated
@ToString(exclude = "password")
class SapProperties {
    /**
     * URL of SAP-PO instance to send request to.
     */
    @NotBlank
    private String endpointUrl;
    /**
     * Username to authenticate against SAP-PO.
     */
    @NotBlank
    private String username;
    /**
     * Password to authenticate against SAP-PO.
     */
    @NotBlank
    private String password;
    /**
     * Regex pattern for parsing filename to different parts.
     * Needs to lead to four matching groups, which are mapped to: document type, pagination nr, box nr
     * and barcode.
     */
    @NotBlank
    private String filenamePattern;
    /**
     * Key to put {@link ParsedFilename#getPaginationNr()} in additional information of created
     * invoices.
     */
    @NotBlank
    private String infoPaginationKey;
    /**
     * Key to put {@link ParsedFilename#getBarcode()} in additional information of created invoices.
     * Only applied for {@link ParsedFilename.DocumentType#RBU}.
     */
    @NotBlank
    private String infoBarcodeKey;
}

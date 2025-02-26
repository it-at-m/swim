package de.muenchen.oss.swim.invoice.adapter.out.sap;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Data
@ConfigurationProperties("swim.sap")
@Validated
class SapProperties {
    /**
     * URL of SAP-PO instance to send request to.
     */
    @NotBlank
    private String endpoint;
    /**
     * Username to authenticated against SAP-PO.
     */
    @NotBlank
    private String username;
    /**
     * Password to authenticated against SAP-PO.
     */
    @NotBlank
    private String password;
    /**
     * Key to put {@link ParsedFilename#paginationNr()} in additional information of created invoices.
     */
    @NotBlank
    private String infoPaginationKey;
    /**
     * Key to put {@link ParsedFilename#barcode()} in additional information of created invoices.
     * Only applied for {@link ParsedFilename.DocumentType#RBU}.
     */
    @NotBlank
    private String infoBarcodeKey;
}

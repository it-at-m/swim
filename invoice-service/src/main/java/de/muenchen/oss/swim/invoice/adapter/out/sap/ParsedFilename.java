package de.muenchen.oss.swim.invoice.adapter.out.sap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class ParsedFilename {
    private static final String SAP_DOCUMENT_TYPE_REC = "ZVIMINVSKA";
    private static final String SAP_DOCUMENT_TYPE_RBU = "ZVIMBARSKA";

    private final DocumentType documentType;
    private final String paginationNr;
    private final String boxNr;
    private final String barcode;

    ParsedFilename(final String documentType, final String paginationNr, final String boxNr, final String barcode) {
        this.documentType = ParsedFilename.DocumentType.valueOf(documentType);
        this.paginationNr = paginationNr;
        this.boxNr = boxNr;
        this.barcode = barcode;
    }

    @RequiredArgsConstructor
    @Getter
    public enum DocumentType {
        REC(SAP_DOCUMENT_TYPE_REC),
        RBU(SAP_DOCUMENT_TYPE_RBU);

        private final String sapValue;
    }
}

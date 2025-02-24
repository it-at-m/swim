package de.muenchen.oss.swim.invoice.adapter.out.sap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

record ParsedFilename(
        DocumentType documentType,
        String paginationNr,
        String boxNr,
        String barcode) {

    private static final String SAP_DOCUMENT_TYPE_REC = "ZVIMINVSKA";
    private static final String SAP_DOCUMENT_TYPE_RBU = "ZVIMBARSKA";

    @RequiredArgsConstructor
    @Getter
    public enum DocumentType {
        REC(SAP_DOCUMENT_TYPE_REC),
        RBU(SAP_DOCUMENT_TYPE_RBU);

        private final String sapValue;
    }
}

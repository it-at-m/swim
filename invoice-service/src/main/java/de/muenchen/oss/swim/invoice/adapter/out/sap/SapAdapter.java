package de.muenchen.oss.swim.invoice.adapter.out.sap;

import de.lhm.pi.erechnung.swm.AdditionalInformation;
import de.lhm.pi.erechnung.swm.DocumentHeader;
import de.lhm.pi.erechnung.swm.InvoiceDocumentRequest;
import de.lhm.pi.erechnung.swm.InvoiceDocumentResponse;
import de.lhm.pi.erechnung.swm.ResponseInformation;
import de.lhm.pi.erechnung.swm.SIInvoiceDocumentSYOB;
import de.muenchen.oss.swim.invoice.application.port.out.InvoiceServiceOutPort;
import de.muenchen.oss.swim.invoice.domain.exception.InvoiceException;
import jakarta.activation.DataHandler;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SapAdapter implements InvoiceServiceOutPort {
    private static final String DOCUMENT_TYPE_PDF = "PDF";

    private final SapProperties sapProperties;
    private final SIInvoiceDocumentSYOB invoiceClient;

    @Override
    public void createInvoice(final String filename, final InputStream inputStream) {
        // parse filename
        final ParsedFilename parsedFilename = parseFilename(filename);
        // build document header
        final DocumentHeader header = new DocumentHeader();
        header.setDocumentType(parsedFilename.getDocumentType().getSapValue());
        final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar());
        header.setScanDate(xmlGregorianCalendar);
        header.setScanTime(xmlGregorianCalendar);
        header.setDocumentName(filename);
        header.setDocumentClass(DOCUMENT_TYPE_PDF);
        // build request
        final InvoiceDocumentRequest request = new InvoiceDocumentRequest();
        request.setDocument(new DataHandler(new InputStreamDataSource(inputStream)));
        request.setDocumentHeader(header);
        // add document type additional information
        this.addAdditionalInformation(request, sapProperties.getInfoPaginationKey(), parsedFilename.getPaginationNr());
        if (parsedFilename.getDocumentType() == ParsedFilename.DocumentType.RBU) {
            this.addAdditionalInformation(request, sapProperties.getInfoBarcodeKey(), parsedFilename.getBarcode());
        }
        // make request
        try {
            final InvoiceDocumentResponse response = invoiceClient.siInvoiceDocumentSYOB(request);
            final ResponseInformation responseInformation = response.getInformation();
            String description = null;
            String type = null;
            if (responseInformation != null) {
                description = responseInformation.getDescription();
                type = responseInformation.getType();
            }
            final String registrationId = response.getRegistrationID();
            log.info("Created invoice with registration id {} (description: {}, type: {})", registrationId, description, type);
        } catch (final SOAPFaultException e) {
            throw new InvoiceException("Create invoice: SOAP request failed", e);
        }
    }

    /**
     * Add additional information to invoice request.
     *
     * @param request The request to add the information to.
     * @param type The type of the value (key).
     * @param value The value to add.
     */
    /* default */ void addAdditionalInformation(final InvoiceDocumentRequest request, final String type, final String value) {
        final AdditionalInformation additionalInformation = new AdditionalInformation();
        additionalInformation.setType(type);
        additionalInformation.setValue(value);
        InvoiceDocumentRequest.AdditionalInformation container = request.getAdditionalInformation();
        if (container == null) {
            container = new InvoiceDocumentRequest.AdditionalInformation();
            request.setAdditionalInformation(container);
        }
        container.getAdditionalInformation().add(additionalInformation);
    }

    /**
     * Parse filename to different parts.
     *
     * @param filename The filename to parse.
     * @return The parsed filename.
     */
    /* default */ ParsedFilename parseFilename(final String filename) {
        final Pattern pattern = Pattern.compile(this.sapProperties.getFilenamePattern(), Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(filename);
        if (!matcher.find()) {
            throw new InvoiceException("Filename did not match the expected format. Expected: <document type>-<pagination nr>-<box nr>-<barcode>.pdf, found: " +
                    filename);
        }
        final ParsedFilename parsedFilename = new ParsedFilename(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                matcher.group(4));
        // document type RBU requires barcode
        if (parsedFilename.getDocumentType() == ParsedFilename.DocumentType.RBU && Strings.isBlank(parsedFilename.getBarcode())) {
            throw new InvoiceException("RBU but no barcode in filename: " + filename);
        }
        return parsedFilename;
    }
}

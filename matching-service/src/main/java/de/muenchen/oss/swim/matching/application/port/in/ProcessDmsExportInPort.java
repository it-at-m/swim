package de.muenchen.oss.swim.matching.application.port.in;

import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.ImportReport;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessDmsExportInPort {
    /**
     * Process an exported list of DMS inboxes.
     *
     * @param csvExport The export in CSV format.
     * @return Report of import.
     * @throws CsvParsingException If the input CSV can't be parsed.
     */
    ImportReport processExport(InputStream csvExport) throws CsvParsingException;

    /**
     * Trigger import of DMS inboxes through export in DMS.
     *
     * @return Report of import.
     */
    ImportReport triggerProcessingViaDms() throws CsvParsingException, IOException;
}

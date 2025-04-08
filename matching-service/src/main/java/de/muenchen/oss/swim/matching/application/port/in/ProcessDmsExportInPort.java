package de.muenchen.oss.swim.matching.application.port.in;

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
     */
    ImportReport processExport(InputStream csvExport) throws IOException;
}

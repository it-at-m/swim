package de.muenchen.swim.matching.application.port.in;

import de.muenchen.swim.matching.domain.model.DmsInbox;
import de.muenchen.swim.matching.domain.model.ImportReport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessDmsExportInPort {
    /**
     * Process list of dms inboxes.
     *
     * @param dmsInboxes Inboxes to be processed.
     * @return Report of process.
     */
    ImportReport process(@Valid List<DmsInbox> dmsInboxes);
}

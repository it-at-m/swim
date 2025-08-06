package de.muenchen.oss.swim.matching.adapter.in.schedule;

import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.ImportReport;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Adapter for triggering scheduled operations (e.g. importing from DMS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAdapter {
    private final ProcessDmsExportInPort processDmsExportInPort;

    /**
     * Trigger the import of matching data from the DMS via cron.
     */
    @Scheduled(cron = "${swim.schedule-cron}")
    public void triggerProcessingViaDms() {
        try {
            final ImportReport report = processDmsExportInPort.triggerProcessingViaDms();
            log.info("Scheduled import from DMS completed successfully: {}", report);
        } catch (CsvParsingException | IOException e) {
            log.error("Scheduled import from DMS failed", e);
        }
    }
}

package de.muenchen.oss.swim.matching.adapter.in.schedule;

import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleAdapter {
    private final ProcessDmsExportInPort processDmsExportInPort;

    @Scheduled(cron = "${swim.schedule-cron}")
    public void triggerProcessingViaDms() {
        processDmsExportInPort.triggerProcessingViaDms();
    }
}

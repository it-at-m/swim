package de.muenchen.oss.swim.dms.adapter.out.schedule;

import de.muenchen.oss.swim.dms.application.port.in.CleanupShadowFilesInPort;
import de.muenchen.oss.swim.dms.configuration.LeaderState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAdapter {
    private final LeaderState leaderState;
    private final CleanupShadowFilesInPort cleanupShadowFilesInPort;

    @Scheduled(cron = "${swim.shadow-file-cleanup.cron}")
    public void triggerShadowFileCleanup() {
        if (leaderState.isLeader()) {
            cleanupShadowFilesInPort.cleanupShadowFiles();
        } else {
            log.info("Skipping shadow file cleanup as not leader");
        }
    }
}

package de.muenchen.oss.swim.dms.adapter.out.schedule;

import de.muenchen.oss.swim.dms.application.port.in.ArchiveShadowFilesInPort;
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
    private final ArchiveShadowFilesInPort archiveShadowFilesInPort;

    @Scheduled(cron = "${swim.shadow-file-archiving.cron}")
    public void triggerArchiveShadowFiles() {
        if (leaderState.isLeader()) {
            archiveShadowFilesInPort.archiveShadowFiles();
        } else {
            log.info("Skipping archiving of shadow file as not leader");
        }
    }
}

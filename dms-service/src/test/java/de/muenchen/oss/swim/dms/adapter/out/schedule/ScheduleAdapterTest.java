package de.muenchen.oss.swim.dms.adapter.out.schedule;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dms.application.port.in.ArchiveShadowFilesInPort;
import de.muenchen.oss.swim.dms.configuration.LeaderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleAdapterTest {

    @Mock
    private LeaderState leaderState;

    @Mock
    private ArchiveShadowFilesInPort archiveShadowFilesInPort;

    @InjectMocks
    private ScheduleAdapter scheduleAdapter;

    @Test
    void triggerArchiveShadowFiles_whenLeader_runs() {
        when(leaderState.isLeader()).thenReturn(true);

        scheduleAdapter.triggerArchiveShadowFiles();

        verify(archiveShadowFilesInPort).archiveShadowFiles();
    }

    @Test
    void triggerArchiveShadowFiles_whenNotLeader_skips() {
        when(leaderState.isLeader()).thenReturn(false);

        scheduleAdapter.triggerArchiveShadowFiles();

        verify(archiveShadowFilesInPort, never()).archiveShadowFiles();
    }
}

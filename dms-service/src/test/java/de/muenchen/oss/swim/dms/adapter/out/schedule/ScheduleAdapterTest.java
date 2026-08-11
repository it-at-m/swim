package de.muenchen.oss.swim.dms.adapter.out.schedule;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dms.application.port.in.CleanupShadowFilesInPort;
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
    private CleanupShadowFilesInPort cleanupShadowFilesInPort;

    @InjectMocks
    private ScheduleAdapter scheduleAdapter;

    @Test
    void triggerShadowFileCleanup_whenLeader_runsCleanup() {
        when(leaderState.isLeader()).thenReturn(true);

        scheduleAdapter.triggerShadowFileCleanup();

        verify(cleanupShadowFilesInPort).cleanupShadowFiles();
    }

    @Test
    void triggerShadowFileCleanup_whenNotLeader_skipsCleanup() {
        when(leaderState.isLeader()).thenReturn(false);

        scheduleAdapter.triggerShadowFileCleanup();

        verify(cleanupShadowFilesInPort, never()).cleanupShadowFiles();
    }
}

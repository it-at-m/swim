package de.muenchen.oss.swim.dms.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeaderStateTest {

    @Test
    void constructor_enabledLeaderElection_initiallyNotLeader() {
        final LeaderState leaderState = new LeaderState(true);

        assertFalse(leaderState.isLeader());
    }

    @Test
    void constructor_disabledLeaderElection_initiallyLeader() {
        final LeaderState leaderState = new LeaderState(false);

        assertTrue(leaderState.isLeader());
    }

    @Test
    void onGranted_setsLeaderTrue() {
        final LeaderState leaderState = new LeaderState(true);

        leaderState.onGranted();

        assertTrue(leaderState.isLeader());
    }

    @Test
    void onRevoked_setsLeaderFalse() {
        final LeaderState leaderState = new LeaderState(true);
        leaderState.onGranted();

        leaderState.onRevoked();

        assertFalse(leaderState.isLeader());
    }
}

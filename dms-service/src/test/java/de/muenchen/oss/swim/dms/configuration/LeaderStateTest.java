package de.muenchen.oss.swim.dms.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.kubernetes.commons.leader.LeaderProperties;

class LeaderStateTest {

    @Test
    void constructor_enabledLeaderProperties_initiallyNotLeader() {
        final LeaderProperties leaderProperties = new LeaderProperties();
        leaderProperties.setEnabled(true);

        final LeaderState leaderState = new LeaderState(Optional.of(leaderProperties));

        assertFalse(leaderState.isLeader());
    }

    @Test
    void constructor_disabledLeaderProperties_initiallyLeader() {
        final LeaderProperties leaderProperties = new LeaderProperties();
        leaderProperties.setEnabled(false);

        final LeaderState leaderState = new LeaderState(Optional.of(leaderProperties));

        assertTrue(leaderState.isLeader());
    }

    @Test
    void constructor_emptyLeaderProperties_initiallyLeader() {
        final LeaderState leaderState = new LeaderState(Optional.empty());

        assertTrue(leaderState.isLeader());
    }

    @Test
    void onGranted_setsLeaderTrue() {
        final LeaderProperties leaderProperties = new LeaderProperties();
        leaderProperties.setEnabled(true);
        final LeaderState leaderState = new LeaderState(Optional.of(leaderProperties));

        leaderState.onGranted();

        assertTrue(leaderState.isLeader());
    }

    @Test
    void onRevoked_setsLeaderFalse() {
        final LeaderProperties leaderProperties = new LeaderProperties();
        leaderProperties.setEnabled(true);
        final LeaderState leaderState = new LeaderState(Optional.of(leaderProperties));
        leaderState.onGranted();

        leaderState.onRevoked();

        assertFalse(leaderState.isLeader());
    }
}

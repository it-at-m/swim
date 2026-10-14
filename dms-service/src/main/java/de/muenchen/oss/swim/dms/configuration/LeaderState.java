package de.muenchen.oss.swim.dms.configuration;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.leader.LeaderUtils;
import org.springframework.cloud.kubernetes.commons.leader.election.events.StartLeadingEvent;
import org.springframework.cloud.kubernetes.commons.leader.election.events.StopLeadingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LeaderState {
    private final AtomicBoolean leader;

    public LeaderState(@Value("${" + LeaderUtils.LEADER_ELECTION_ENABLED_PROPERTY + "}") final boolean leaderElectionEnabled) {
        if (leaderElectionEnabled) {
            log.info("Kubernetes leader election is enabled, waiting for events");
            this.leader = new AtomicBoolean(false);
        } else {
            log.warn("Kubernetes leader election is disabled, each instance becomes a leader");
            this.leader = new AtomicBoolean(true);
        }
    }

    @EventListener(StartLeadingEvent.class)
    public void onGranted() {
        log.info("Became leader");
        this.leader.set(true);
    }

    @EventListener(StopLeadingEvent.class)
    public void onRevoked() {
        log.info("Lost leadership");
        this.leader.set(false);
    }

    public boolean isLeader() {
        return this.leader.get();
    }
}

package de.muenchen.oss.swim.dms.configuration;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.kubernetes.commons.leader.LeaderProperties;
import org.springframework.context.event.EventListener;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LeaderState {
    private final AtomicBoolean leader;

    public LeaderState(final Optional<LeaderProperties> leaderProperties) {
        if (leaderProperties.isEmpty() || !leaderProperties.get().isEnabled()) {
            this.leader = new AtomicBoolean(true);
            log.warn("Leader state is disabled, each instance becomes a leader");
        } else {
            this.leader = new AtomicBoolean(false);
        }
    }

    @EventListener(OnGrantedEvent.class)
    public void onGranted() {
        log.info("Became leader");
        this.leader.set(true);
    }

    @EventListener(OnRevokedEvent.class)
    public void onRevoked() {
        log.info("Lost leadership");
        this.leader.set(false);
    }

    public boolean isLeader() {
        return this.leader.get();
    }
}

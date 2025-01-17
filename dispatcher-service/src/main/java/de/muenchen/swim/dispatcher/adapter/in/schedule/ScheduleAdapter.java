package de.muenchen.swim.dispatcher.adapter.in.schedule;

import de.muenchen.swim.dispatcher.application.port.in.DispatcherInPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleAdapter {
    private final DispatcherInPort dispatcherInPort;

    @Scheduled(cron = "${swim.dispatching-cron}")
    public void triggerDispatching() {
        dispatcherInPort.triggerDispatching();
    }

    @Scheduled(cron = "${swim.protocol-processing-cron}")
    public void triggerProtocolProcessing() {
        dispatcherInPort.triggerProtocolProcessing();
    }
}

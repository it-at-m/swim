package de.muenchen.oss.swim.dispatcher.adapter.in.schedule;

import de.muenchen.oss.swim.dispatcher.application.port.in.DispatcherInPort;
import de.muenchen.oss.swim.dispatcher.application.port.in.ProtocolProcessingInPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleAdapter {
    private final DispatcherInPort dispatcherInPort;
    private final ProtocolProcessingInPort protocolProcessingInPort;

    @Scheduled(cron = "${swim.dispatching-cron}")
    public void triggerDispatching() {
        dispatcherInPort.triggerDispatching();
    }

    @Scheduled(cron = "${swim.protocol-processing-cron}")
    public void triggerProtocolProcessing() {
        protocolProcessingInPort.triggerProtocolProcessing();
    }
}

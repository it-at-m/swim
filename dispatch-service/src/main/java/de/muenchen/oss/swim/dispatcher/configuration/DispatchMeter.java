package de.muenchen.oss.swim.dispatcher.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DispatchMeter {
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> dispatchedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> finishedCounters = new ConcurrentHashMap<>();

    /**
     * Increment counter metric of successfully dispatched files.
     *
     * @param useCase The use case of the file.
     * @param destination The destination the file was dispatched to.
     */
    public void incrementDispatched(final String useCase, final String destination) {
        final String key = useCase + "_" + destination;
        final Counter counter = dispatchedCounters.computeIfAbsent(key, k -> Counter.builder("swim_dispatch_dispatched_count")
                .tag("use-case", useCase)
                .tag("destination", destination)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Increment counter metric of failed files.
     *
     * @param useCase The use case of the file.
     * @param source The source where the error occurred.
     */
    public void incrementError(final String useCase, final String source) {
        final String key = useCase + "_" + source;
        final Counter counter = errorCounters.computeIfAbsent(key, k -> Counter.builder("swim_dispatch_error_count")
                .tag("use-case", useCase)
                .tag("source", source)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Increment counter metric for finished files.
     *
     * @param useCase The use case of the file.
     */
    public void incrementFinished(final String useCase) {
        final Counter counter = finishedCounters.computeIfAbsent(useCase, k -> Counter.builder("swim_dispatch_finished_count")
                .tag("use-case", useCase)
                .register(meterRegistry));
        counter.increment();
    }
}

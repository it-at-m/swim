package de.muenchen.oss.swim.dipa.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DipaMeter {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();

    /**
     * Increment counter metric of successfully processed files.
     *
     * @param useCase The use case of the file.
     * @param type The type of resource created.
     */
    public void incrementProcessed(final String useCase, final String type) {
        final String key = useCase + "_" + type;
        final Counter counter = processedCounters.computeIfAbsent(key, k -> Counter.builder("swim_dipa_processed_count")
                .tag("use-case", useCase)
                .tag("dipa-type", type)
                .register(meterRegistry));
        counter.increment();
    }
}

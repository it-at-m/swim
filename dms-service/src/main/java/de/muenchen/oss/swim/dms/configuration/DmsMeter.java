package de.muenchen.oss.swim.dms.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmsMeter {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();

    /**
     * Increment counter metric of successfully processed files.
     *
     * @param useCase The use case of the file.
     * @param type The type of DMS ressource created.
     */
    public void incrementProcessed(final String useCase, final String type) {
        final String key = useCase + "_" + type;
        final Counter counter = processedCounters.computeIfAbsent(key, k -> Counter.builder("swim_dms_processed_count")
                .tag("use-case", useCase)
                .tag("dms-type", type)
                .register(meterRegistry));
        counter.increment();
    }
}

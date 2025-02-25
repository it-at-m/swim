package de.muenchen.oss.swim.invoice.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceMeter {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();

    /**
     * Increment counter metric of successfully processed files.
     *
     * @param useCase The use case of the file.
     */
    public void incrementProcessed(final String useCase) {
        final Counter counter = processedCounters.computeIfAbsent(useCase, k -> Counter.builder("swim_invoice_processed_count")
                .tag("use-case", useCase)
                .register(meterRegistry));
        counter.increment();
    }
}

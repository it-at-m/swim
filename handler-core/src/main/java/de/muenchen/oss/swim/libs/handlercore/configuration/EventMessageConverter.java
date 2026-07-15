package de.muenchen.oss.swim.libs.handlercore.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.StreamingException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;

/**
 * Spring {@link MessageConverter} that converts to and from {@link FileEvent}
 * implementations based on a discriminator header.
 * <p>
 * ATTENTION: This class needs to match the one in the dispatch-service.
 *
 * <p>
 * When the requested target type is {@code FileEvent}, the converter inspects the
 * {@value #TYPE_HEADER} header to resolve the concrete implementation:
 * <ul>
 * <li>{@value SingleFileEvent#TYPE_NAME} &rarr; {@link SingleFileEvent}</li>
 * <li>{@value MultiFileEvent#TYPE_NAME} &rarr; {@link MultiFileEvent}</li>
 * </ul>
 * If the header is missing it falls back to {@link SingleFileEvent} and, if it's unknown,
 * this converter returns {@code null} so that other converters may attempt conversion.
 *
 * <p>
 * Inbound conversion expects a JSON {@code byte[]} payload.
 *
 * <p>
 * Outbound conversion is supported for
 * {@link FileEvent} payloads only: the payload is serialized to JSON and the
 * {@value #TYPE_HEADER} is set to the corresponding {@code TYPE_NAME}. For non-{@code FileEvent}
 * payloads {@code null} is returned.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventMessageConverter implements MessageConverter {
    public static final String TYPE_HEADER = "swim_event_type";

    private final ObjectMapper objectMapper;

    @Override
    public Object fromMessage(@NonNull final Message<?> message, @NonNull final Class<?> targetClass) {
        final Class<?> resolvedTargetClass = this.resolveTargetType(message, targetClass);
        // if target type couldn't be resolved skip message converter
        if (resolvedTargetClass == null) {
            return null;
        }
        return convert(message, resolvedTargetClass);
    }

    @Override
    public Message<?> toMessage(@NonNull final Object payload, final MessageHeaders headers) {
        // skip everything except FileEvents
        if (!(payload instanceof FileEvent)) {
            return null;
        }
        // convert to json
        final String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Payload couldn't be converted to JSON", e);
        }
        // build message
        final MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(jsonPayload.getBytes(StandardCharsets.UTF_8));
        if (headers != null) {
            messageBuilder.copyHeaders(headers);
        }
        messageBuilder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, "application/json");
        final String eventType;
        if (payload instanceof SingleFileEvent) {
            eventType = SingleFileEvent.TYPE_NAME;
        } else if (payload instanceof MultiFileEvent) {
            eventType = MultiFileEvent.TYPE_NAME;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported FileEvent implementation: " + payload.getClass().getName());
        }
        messageBuilder.setHeader(TYPE_HEADER, eventType);
        return messageBuilder.build();
    }

    /**
     * Resolves the concrete target class for a {@link FileEvent} based on the
     * {@value #TYPE_HEADER} header. Returns {@code null} if the requested target class is not
     * {@code FileEvent} or the type header is unknown so other converters can handle it.
     *
     * @throws StreamingException if the type header can't be parsed to a String.
     */
    private Class<?> resolveTargetType(final Message<?> message, final Class<?> targetClass) {
        if (!FileEvent.class.equals(targetClass)) {
            return null;
        }
        final String type;
        try {
            type = message.getHeaders().get(TYPE_HEADER, String.class);
        } catch (final IllegalArgumentException e) {
            throw new StreamingException("Type header couldn't be resolved to String", e);
        }
        // fallback to single if no type
        if (type == null || SingleFileEvent.TYPE_NAME.equals(type)) {
            return SingleFileEvent.class;
        } else if (MultiFileEvent.TYPE_NAME.equals(type)) {
            return MultiFileEvent.class;
        }
        return null;
    }

    /**
     * Converts the message payload (expected to be a JSON {@code byte[]}) into the resolved
     * {@link FileEvent} implementation using the {@link ObjectMapper}.
     *
     * @throws IllegalArgumentException if the payload type is unsupported.
     * @throws StreamingException if JSON deserialization fails.
     */
    private FileEvent convert(final Message<?> message, final Class<?> targetClass) {
        final Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            try {
                return (FileEvent) objectMapper.readValue((byte[]) payload, targetClass);
            } catch (final IOException e) {
                throw new StreamingException("Error while converting JSON to FileEvent", e);
            }
        } else {
            throw new IllegalArgumentException("Payload type '%s' could not be converted to FileEvent".formatted(payload.getClass().getName()));
        }
    }
}

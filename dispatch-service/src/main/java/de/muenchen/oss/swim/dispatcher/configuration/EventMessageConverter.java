package de.muenchen.oss.swim.dispatcher.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dispatcher.domain.exception.StreamingException;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.FileEvent;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.FileEventDTO;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.MultiFileEventDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Spring {@link MessageConverter} that deserializes incoming streaming messages into
 * {@link FileEvent} implementations based on a discriminator header.
 *
 * <p>
 * When the requested target type is {@code FileEvent}, the converter inspects the
 * {@value #TYPE_HEADER} header to resolve the concrete implementation:
 * <ul>
 * <li>"single" &rarr; {@link FileEventDTO}</li>
 * <li>"multi" &rarr; {@link MultiFileEventDTO}</li>
 * </ul>
 * If the header is missing it falls back to "single" and if it's unknown, this converter returns
 * {@code null} so that other
 * converters may attempt conversion.
 * </p>
 *
 * <p>
 * The payload is expected to be a {@code byte[]} containing JSON and is converted using
 * the injected {@link ObjectMapper}. Outbound conversion via
 * {@link #toMessage(Object, MessageHeaders)}
 * is not supported and returns {@code null}.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventMessageConverter implements MessageConverter {
    public final static String TYPE_HEADER = "swim_event_type";

    private final ObjectMapper objectMapper;

    @Override
    public @Nullable
    Object fromMessage(@NotNull final Message<?> message, @NotNull final Class<?> targetClass) {
        final Class<?> resolvedTargetClass = this.resolveTargetType(message, targetClass);
        // if target type couldn't be resolved skip message converter
        if (resolvedTargetClass == null) {
            return null;
        }
        return convert(message, resolvedTargetClass);
    }

    @Override
    public @Nullable
    Message<?> toMessage(@NotNull final Object payload, @Nullable MessageHeaders headers) {
        // skip everything except FileEvents
        if (!FileEvent.class.isAssignableFrom(payload.getClass())) {
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
        messageBuilder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, "application/json");
        messageBuilder.setHeader(TYPE_HEADER,
                payload instanceof FileEventDTO ? FileEventDTO.TYPE_NAME : payload instanceof MultiFileEventDTO ? MultiFileEventDTO.TYPE_NAME : null);
        return messageBuilder.build();
    }

    /**
     * Resolves the concrete target class for a {@link FileEvent} based on the
     * {@value #TYPE_HEADER} header. Returns {@code null} if the requested target type is not
     * {@code FileEvent} or the header is unknown so other converters can handle it.
     */
    private Class<?> resolveTargetType(final Message<?> message, final Class<?> targetClass) {
        if (!FileEvent.class.equals(targetClass)) {
            return null;
        }
        final String type;
        try {
            type = message.getHeaders().get(TYPE_HEADER, String.class);
        } catch (final RuntimeException e) {
            throw new StreamingException("Type header couldn't be resolved", e);
        }
        // fallback to single if no type
        if (type == null || FileEventDTO.TYPE_NAME.equals(type)) {
            return FileEventDTO.class;
        } else if (MultiFileEventDTO.TYPE_NAME.equals(type)) {
            return MultiFileEventDTO.class;
        }
        return null;
    }

    /**
     * Converts the message payload (expected to be a JSON {@code byte[]}) into the resolved
     * {@link FileEvent} implementation using the {@link ObjectMapper}.
     *
     * @throws IllegalArgumentException if the payload type is unsupported.
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
            throw new IllegalArgumentException("Argument type '%s' could not be converted to FileEvent".formatted(targetClass.getName()));
        }
    }
}

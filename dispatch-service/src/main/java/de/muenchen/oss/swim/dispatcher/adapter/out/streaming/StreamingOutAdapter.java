package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.domain.exception.StreamingException;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingOutAdapter implements FileDispatchingOutPort {
    private final StreamBridge streamBridge;

    @Override
    public void dispatchFile(final String bindingName, final String useCase, final PresignedFile presignedFile) {
        final FileEventDTO event = FileEventDTO.fromPresignedFile(useCase, presignedFile);
        if (!streamBridge.send(bindingName, event)) {
            throw new StreamingException("Event couldn't be sent");
        }
    }
}

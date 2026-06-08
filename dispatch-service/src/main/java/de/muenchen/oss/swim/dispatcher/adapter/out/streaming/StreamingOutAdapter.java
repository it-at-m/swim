package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.domain.exception.StreamingException;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.FileEventDTO;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.MultiFileEventDTO;
import java.util.List;
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
        send(bindingName, event);
    }

    @Override
    public void dispatchFile(final String bindingName, final String useCase, final List<PresignedFile> presignedFiles) {
        final MultiFileEventDTO event = new MultiFileEventDTO(useCase, presignedFiles);
        send(bindingName, event);
    }

    private void send(final String bindingName, final Object event) {
        if (!streamBridge.send(bindingName, event)) {
            throw new StreamingException("Event couldn't be sent");
        }
    }
}

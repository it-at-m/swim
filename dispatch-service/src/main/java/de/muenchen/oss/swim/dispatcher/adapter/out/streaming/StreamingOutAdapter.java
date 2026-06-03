package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.domain.exception.StreamingException;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingOutAdapter implements FileDispatchingOutPort {
    private final StreamBridge streamBridge;

    @Override
    public void dispatchFile(final String bindingName, final String useCase, final List<PresignedFile> presignedFiles) {
        final Object event;
        if (presignedFiles.size() == 1) {
            event = FileEventDTO.fromPresignedFile(useCase, presignedFiles.getFirst());
        } else {
            event = new MultiFileEventDTO(useCase, presignedFiles);
        }
        if (!streamBridge.send(bindingName, event)) {
            throw new StreamingException("Event couldn't be sent");
        }
    }
}

package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingOutAdapter implements FileDispatchingOutPort {
    private final StreamBridge streamBridge;

    @Override
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public void dispatchFile(final String bindingName, final String useCase, final String presignedUrl, final String metadataPresignedUrl) {
        final FileEventDTO event = new FileEventDTO(useCase, presignedUrl, metadataPresignedUrl);
        streamBridge.send(bindingName, event);
    }
}

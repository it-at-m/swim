package de.muenchen.oss.swim.dms.adapter.out.streaming;

import de.muenchen.oss.swim.dms.application.port.out.FileEventOutPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingOutAdapter implements FileEventOutPort {
    private final StreamBridge streamBridge;

    @Override
    public void fileFinished(final String useCase, final String presignedUrl, final String metadataPresignedUrl) {
        final FileFinishedEventDTO event = new FileFinishedEventDTO(useCase, presignedUrl, metadataPresignedUrl);
        streamBridge.send("finished-out", event);
    }
}

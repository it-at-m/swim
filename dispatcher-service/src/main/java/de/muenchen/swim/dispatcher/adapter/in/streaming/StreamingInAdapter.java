package de.muenchen.swim.dispatcher.adapter.in.streaming;

import de.muenchen.swim.dispatcher.application.port.in.MarkFileFinishedInPort;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingInAdapter {
    private final MarkFileFinishedInPort markFileFinishedInPort;

    @Bean
    protected Consumer<Message<FileFinishedDTO>> finished() {
        return fileFinishedEventDTOMessage -> {
            final FileFinishedDTO fileFinishedDTO = fileFinishedEventDTOMessage.getPayload();
            markFileFinishedInPort.markFileFinished(fileFinishedDTO.useCase(), fileFinishedDTO.presignedUrl());
            if (Strings.isNotBlank(fileFinishedDTO.metadataPresignedUrl())) {
                markFileFinishedInPort.markFileFinished(fileFinishedDTO.useCase(), fileFinishedDTO.metadataPresignedUrl());
            }
        };
    }
}

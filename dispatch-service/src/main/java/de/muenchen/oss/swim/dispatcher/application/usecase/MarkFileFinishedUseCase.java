package de.muenchen.oss.swim.dispatcher.application.usecase;

import de.muenchen.oss.swim.dispatcher.application.port.in.MarkFileFinishedInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.FileEvent;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkFileFinishedUseCase implements MarkFileFinishedInPort {
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileHandlingHelper fileHandlingHelper;

    @Override
    public void markFileFinished(final FileEvent event) throws PresignedUrlException, UseCaseException {
        // resolve usecase from name
        final UseCase useCase = this.swimDispatcherProperties.findUseCase(event.useCase());
        // verify presigned url and extract File
        final File file = this.fileSystemOutPort.verifyAndResolvePresignedUrl(useCase, event.presignedUrl());
        // finish file
        fileHandlingHelper.finishFile(useCase, file.tenant(), file.bucket(), file.path());
        // finish metadata file
        if (Strings.isNotBlank(event.metadataPresignedUrl())) {
            // verify presigned url and extract metadata File
            final File metadataFile = this.fileSystemOutPort.verifyAndResolvePresignedUrl(useCase, event.metadataPresignedUrl());
            // finish metadata file
            fileHandlingHelper.finishFile(useCase, metadataFile.tenant(), metadataFile.bucket(), metadataFile.path());
        }
    }
}

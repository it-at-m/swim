package de.muenchen.oss.swim.dispatcher.application.usecase;

import de.muenchen.oss.swim.dispatcher.application.port.in.MarkFileFinishedInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkFileFinishedUseCase implements MarkFileFinishedInPort {
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;

    @Override
    public void markFileFinished(final String useCaseName, final String presignedUrl) throws PresignedUrlException, UseCaseException {
        // resolve usecase from name
        final UseCase useCase = this.swimDispatcherProperties.findUseCase(useCaseName);
        // verify presigned url
        if (!fileSystemOutPort.verifyPresignedUrl(presignedUrl)) {
            throw new PresignedUrlException("Presigned url not valid");
        }
        // extract bucket and file path from presigned url
        final File file = File.fromPresignedUrl(presignedUrl);
        // tag file as finished
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchFileFinishedTagValue()));
        // move file
        final String useCasePath = useCase.getPath().endsWith("/") ? useCase.getPath() : useCase.getPath() + "/";
        final String finishedFolder = swimDispatcherProperties.getFinishedFolder().endsWith("/") ? swimDispatcherProperties.getFinishedFolder()
                : swimDispatcherProperties.getFinishedFolder() + "/";
        final String destPath = file.path().replace(useCasePath, String.format("%s%s", useCasePath, finishedFolder));
        fileSystemOutPort.moveFile(file.bucket(), file.path(), destPath);
        log.info("Finished file {} in use case {}", file.path(), useCaseName);
    }
}

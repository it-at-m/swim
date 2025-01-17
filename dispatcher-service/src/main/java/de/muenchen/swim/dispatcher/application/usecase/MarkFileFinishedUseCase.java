package de.muenchen.swim.dispatcher.application.usecase;

import de.muenchen.swim.dispatcher.application.port.in.MarkFileFinishedInPort;
import de.muenchen.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.swim.dispatcher.domain.model.UseCase;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
    private final NotificationOutPort notificationOutPort;

    @Override
    public void markFileFinished(final String useCaseName, final String presignedUrl) {
        // resolve usecase from name
        final UseCase useCase;
        try {
            useCase = this.findUseCase(useCaseName);
        } catch (final UseCaseException e) {
            log.error("UseCase {} couldn't be resolved", useCaseName, e);
            this.notificationOutPort.sendFileFinishError(List.of(swimDispatcherProperties.getFallbackMail()), useCaseName, presignedUrl, e);
            return;
        }
        // mark file as finished
        try {
            this.finishFile(useCaseName, presignedUrl);
        } catch (final PresignedUrlException e) {
            log.error("Error while marking file as finished. Usecase {}, PresignedUrl {}", useCaseName, presignedUrl, e);
            this.notificationOutPort.sendFileFinishError(useCase.getMailAddresses(), useCaseName, presignedUrl, e);
        }
    }

    protected void finishFile(final String useCase, final String presignedUrl) throws PresignedUrlException {
        // verify presigned url
        if (!fileSystemOutPort.verifyPresignedUrl(presignedUrl)) {
            throw new PresignedUrlException("Presigned url not valid");
        }
        // extract bucket and file path from presigned url
        final URI uri;
        try {
            uri = new URI(presignedUrl);
        } catch (final URISyntaxException e) {
            throw new PresignedUrlException("Presigned url could not be parsed", e);
        }
        final String uriPath = uri.getPath().replaceFirst("^/", "");
        final int slashIndex = uriPath.indexOf('/');
        final String bucket = uriPath.substring(0, slashIndex);
        final String filePath = uriPath.substring(slashIndex + 1);
        // tag file as finished
        fileSystemOutPort.tagFile(bucket, filePath, Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchFileFinishedTagValue()));
        log.info("Marked file {} in use case {} as finished", filePath, useCase);
    }

    /**
     * Finde UseCase via name.
     *
     * @param useCaseName Name of the UseCase to find.
     * @return The first UseCase with the given name.
     * @throws UseCaseException If no UseCase was found.
     */
    protected UseCase findUseCase(final String useCaseName) throws UseCaseException {
        return this.swimDispatcherProperties.getUseCases().stream()
                .filter(i -> i.getName().equals(useCaseName))
                .findFirst().orElseThrow(() -> new UseCaseException("Unknown use case " + useCaseName));
    }
}

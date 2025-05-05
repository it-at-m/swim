package de.muenchen.oss.swim.dipa.application.usecase;

import de.muenchen.oss.swim.dipa.application.port.out.DipaOutPort;
import de.muenchen.oss.swim.dipa.configuration.DipaMeter;
import de.muenchen.oss.swim.dipa.configuration.SwimDipaProperties;
import de.muenchen.oss.swim.dipa.domain.model.UseCase;
import de.muenchen.oss.swim.dipa.domain.model.UseCaseSource;
import de.muenchen.oss.swim.dipa.domain.model.dipa.ContentObjectRequest;
import de.muenchen.oss.swim.dipa.domain.model.dipa.HrSubfileContext;
import de.muenchen.oss.swim.dipa.domain.model.dipa.IncomingRequest;
import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    private final SwimDipaProperties swimDipaProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final DipaOutPort dipaOutPort;
    private final PatternHelper patternHelper;
    private final DipaMeter dipaMeter;

    @Override
    public void processFile(final FileEvent event, final File file) throws UnknownUseCaseException, PresignedUrlException {
        log.info("Processing file {} for use case {}", file, event.useCase());
        final UseCase useCase = swimDipaProperties.findUseCase(event.useCase());
        log.debug("Resolved use case: {}", useCase);
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(event.presignedUrl())) {
            switch (useCase.getType()) {
            case HR_SUBFILE_INCOMING -> this.processHrSubfileIncoming(useCase, file, fileStream);
            case null, default -> throw new IllegalStateException("UseCaseType not implemented");
            }
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(event);
        log.info("File {} in use case {} finished", file, useCase.getName());
        // update metric
        dipaMeter.incrementProcessed(useCase.getName(), useCase.getType().name());
    }

    protected void processHrSubfileIncoming(final UseCase useCase, final File file, final InputStream fileContent) {
        final HrSubfileContext requestContext = this.buildHrSubfileContext(useCase, file);
        final IncomingRequest request = this.buildIncomingRequest(useCase, file, fileContent);
        this.dipaOutPort.createHrSubfileIncoming(requestContext, request);
    }

    protected IncomingRequest buildIncomingRequest(final UseCase useCase, final File file, final InputStream fileContent) {
        final String incomingSubject = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingSubjPattern(), file.getFileNameWithoutExtension(),
                null);
        final ContentObjectRequest contentObject = this.buildContentObjectRequest(useCase, file, fileContent);
        return new IncomingRequest(incomingSubject, contentObject);
    }

    protected ContentObjectRequest buildContentObjectRequest(final UseCase useCase, final File file, final InputStream fileContent) {
        final String contentObjectName = String.format("%s.%s",
                this.patternHelper.applyPattern(useCase.getContentObject().getFilenameOverwritePattern(), file.getFileNameWithoutExtension(), null),
                file.getFileExtension());
        return new ContentObjectRequest(contentObjectName, fileContent);
    }

    protected HrSubfileContext buildHrSubfileContext(final UseCase useCase, final File file) {
        final UseCaseSource source = useCase.getCooSource();
        return switch (useCase.getCooSource().getType()) {
        case STATIC -> new HrSubfileContext(useCase.getContext(), source.getStaticPersNr(), source.getStaticCategory());
        case FILENAME -> {
            final String persNr = this.patternHelper.applyPattern(source.getFilenamePersNrPattern(), file.getFileNameWithoutExtension(), null);
            final String category = this.patternHelper.applyPattern(source.getFilenameCategoryPattern(), file.getFileNameWithoutExtension(), null);
            yield new HrSubfileContext(useCase.getContext(), persNr, category);
        }
        };
    }
}

package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import static de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction.DISPATCH;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSystemAccessException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.helper.MetadataHelper;
import de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.Metadata;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchActionsHelper {
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileDispatchingOutPort fileDispatchingOutPort;
    private final FileHandlingHelper fileHandlingHelper;
    private final MetadataHelper metadataHelper;

    /**
     * Resolve dispatch action from tags.
     * If tag isn't present defaults to {@link DispatchAction#DISPATCH}.
     *
     * @param tags The tags of the file.
     * @return The resolved action.
     */
    public @NotNull
    DispatchAction resolveDispatchAction(final Map<String, String> tags) {
        // dispatch if tag doesn't exist
        if (!tags.containsKey(swimDispatcherProperties.getDispatchActionTagKey())) {
            return DISPATCH;
        }
        // parse tag value
        final String actionString = tags.get(swimDispatcherProperties.getDispatchActionTagKey());
        if (actionString == null) {
            throw new IllegalStateException("Action tag value cannot be null");
        }
        return DispatchAction.valueOf(actionString.toUpperCase(Locale.ROOT));
    }

    /**
     * Resolve destination binding either via metadata file or use case.
     * With multiple files use case is used always.
     * See {@link UseCase#isOverwriteDestinationViaMetadata()} and
     * {@link UseCase#getDestinationBinding()}.
     *
     * @param useCase The use case to resolve the destination binding for.
     * @param files The files to resolve the destination binding for.
     * @return The resolved destination binding.
     * @throws MetadataException If metadata file can't be loaded or parsed.
     */
    public String resolveDestinationBinding(final UseCase useCase, final List<FileWithMetadata> files) throws MetadataException {
        // if multiple files ignore all overwrites
        if (files.size() > 1) {
            return useCase.getDestinationBinding();
        }
        final FileReference file = files.getFirst().reference();
        // resolve via metadata file if enabled
        if (useCase.isOverwriteDestinationViaMetadata()) {
            try (InputStream metadataFileStream = this.fileSystemOutPort.readFile(file.getMetadataFile())) {
                final Metadata metadata = metadataHelper.parseMetadataFile(metadataFileStream);
                final String value = metadata.indexFields().get(swimDispatcherProperties.getMetadataDispatchBindingKey());
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            } catch (final FileSystemAccessException | IOException e) {
                throw new MetadataException("Destination via metadata: Error while loading metadata file", e);
            }
        }
        // use UseCase destination binding
        return useCase.getDestinationBinding();
    }

    /**
     * Dispatch multiple files or one to the use case destination binding.
     *
     * @param useCase The use case of the file.
     * @param files The files.
     * @param destination Destination binding to dispatch to.
     * @throws MetadataException If metadata file required but could not be loaded.
     */
    public void dispatchFileGroup(final UseCase useCase, final List<FileWithMetadata> files, final String destination) throws MetadataException {
        // check metadata file exists if required
        final List<PresignedFile> presignedFiles = new ArrayList<>();
        for (final FileWithMetadata fileWithMeta : files) {
            final FileReference file = fileWithMeta.reference();
            final String metadataPresignedUrl;
            if (useCase.isRequiresMetadata()) {
                // build metadata file path
                final FileReference metadataFile = file.getMetadataFile();
                // continue if not existing
                if (!fileSystemOutPort.fileExists(metadataFile)) {
                    final String message = String.format("Metadata file %s missing", metadataFile);
                    throw new MetadataException(message);
                }
                metadataPresignedUrl = fileSystemOutPort.getPresignedUrl(metadataFile);
            } else {
                metadataPresignedUrl = null;
            }
            final String presignedUrl = fileSystemOutPort.getPresignedUrl(file);
            presignedFiles.add(new PresignedFile(presignedUrl, metadataPresignedUrl));
        }
        // dispatch file
        fileDispatchingOutPort.dispatchFile(destination, useCase.getName(), presignedFiles);
        // mark file as dispatched
        for (final FileWithMetadata fileWithMeta : files) {
            final FileReference file = fileWithMeta.reference();
            fileSystemOutPort.tagFile(file, Map.of(
                    swimDispatcherProperties.getDispatchStateTagKey(),
                    swimDispatcherProperties.getDispatchedStateTagValue()));
        }
    }

    /**
     * Reroute a file to another use case.
     *
     * @param useCase The use case of the file.
     * @param file The file to reroute
     * @param tags The tags of the file. Used for resolving target.
     * @throws UseCaseException If target use case can't be resolved.
     */
    public void rerouteFileToUseCase(final UseCase useCase, final FileReference file, final Map<String, String> tags) throws UseCaseException {
        // resolve target use case
        final String targetUseCaseName = tags.get(swimDispatcherProperties.getDispatchActionDestinationTagKey());
        if (targetUseCaseName == null) {
            final String message = String.format("Reroute action failed: No target use case specified in tag '%s' for file %s in use case %s",
                    swimDispatcherProperties.getDispatchActionDestinationTagKey(), file.path(), useCase.getName());
            throw new IllegalStateException(message);
        }
        if (useCase.getName().equals(targetUseCaseName)) {
            final String message = String.format("Reroute action failed: Cannot reroute file %s to the same use case '%s'",
                    file.path(), useCase.getName());
            throw new IllegalStateException(message);
        }
        final UseCase targetUseCase;
        try {
            targetUseCase = swimDispatcherProperties.findUseCase(targetUseCaseName);
        } catch (final UseCaseException e) {
            final String message = String.format("Reroute action failed: Unknown use case %s", targetUseCaseName);
            throw new UseCaseException(message, e);
        }
        // copy file to target use case
        final String rawPath = useCase.getRawPath(swimDispatcherProperties, file.path());
        final String destPath = String.format("%s/from_%s/%s", targetUseCase.getDispatchPath(swimDispatcherProperties), useCase.getName(), rawPath);
        final FileReference destFile = new FileReference(targetUseCase.getBucket(), destPath);
        fileSystemOutPort.copyFile(file, destFile, true);
        // tag protocol processed if enabled
        if (targetUseCase.isTagProtocolProcessed()) {
            fileSystemOutPort.tagFile(destFile, Map.of(
                    swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getProtocolProcessedFilesStateTagValue()));
        }
        // finish file
        this.fileHandlingHelper.finishFile(useCase, file);
        log.info("File {} rerouted from use case {} to use case {}", file, useCase.getName(),
                targetUseCase.getName());
    }
}

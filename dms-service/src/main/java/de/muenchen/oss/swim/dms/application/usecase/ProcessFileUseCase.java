package de.muenchen.oss.swim.dms.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dms.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.File;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    public static final String METADATA_INDEX_FIELDS_KEY = "IndexFields";
    public static final String METADATA_INBOX_COO_KEY = "Postkorb-COO-Adresse";
    public static final String METADATA_USERNAME_KEY = "Username";
    public static final String PATTERN_JOINER = "-";

    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final DmsOutPort dmsOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final ObjectMapper objectMapper;

    @Override
    public void processFile(final String useCaseName, final File file, final String presignedUrl, final String metadataPresignedUrl) {
        log.info("Processing file {} for use case {}", file, useCaseName);
        final UseCase useCase = findUseCase(useCaseName);
        log.debug("Resolved use case: {}", useCase);
        // load file
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(presignedUrl)) {
            // get target coo
            final DmsTarget dmsTarget = resolveTargetCoo(metadataPresignedUrl, useCase, file);
            log.debug("Resolved dms target: {}", dmsTarget);
            // get filename
            final String filename = this.applyOverwritePattern(useCase.getFilenameOverwritePattern(), file.getFileName(), PATTERN_JOINER);
            // transfer to dms
            switch (useCase.getType()) {
            // to dms inbox
            case INBOX -> dmsOutPort.putFileInInbox(dmsTarget, filename, fileStream);
            // create dms incoming
            case INCOMING_OBJECT -> {
                // resolve name for IncomingObject
                final String incomingObjectName;
                if (Strings.isBlank(useCase.getContentObjectNamePattern())) {
                    // use overwritten filename if no pattern for IncomingObject name is defined
                    incomingObjectName = filename;
                } else {
                    // else apply pattern to original filename
                    incomingObjectName = this.applyOverwritePattern(useCase.getContentObjectNamePattern(), file.getFileName(), PATTERN_JOINER);
                }
                // create Incoming
                dmsOutPort.createIncoming(dmsTarget, filename, incomingObjectName, fileStream);
            }
            }
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(useCaseName, presignedUrl, metadataPresignedUrl);
    }

    /**
     * Resolve target coo for useCase.
     * {@link UseCase.Type}
     *
     * @param metadataPresignedUrl Presigned url for metadata file.
     * @param useCase The use case.
     * @return The resolved coo.
     */
    protected DmsTarget resolveTargetCoo(final String metadataPresignedUrl, final UseCase useCase, final File file) {
        return switch (useCase.getCooSource()) {
        case METADATA_FILE -> this.resolveMetadataTargetCoo(metadataPresignedUrl, useCase);
        // TODO resolve coo from filename
        case FILENAME -> throw new UnsupportedOperationException("Coo source type filename not implemented yet");
        case STATIC -> new DmsTarget(useCase.getTargetCoo(), useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        case OU_DEFAULT -> new DmsTarget(null, useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        };
    }

    /**
     * Resolve DmsTarget via metadata file.
     *
     * @param metadataPresignedUrl Presigned url of the metadata file.
     * @param useCase UseCase of the file.
     * @return Resolved DmsTarget.
     */
    protected DmsTarget resolveMetadataTargetCoo(final String metadataPresignedUrl, final UseCase useCase) {
        // validate metadata presigned url provided
        if (Strings.isBlank(metadataPresignedUrl)) {
            throw new MetadataException("Metadata presigned url empty but required");
        }
        // get metadata file
        try (InputStream metadataStream = fileSystemOutPort.getPresignedUrlFile(metadataPresignedUrl)) {
            // extract coo and username from metadata
            final DmsTarget metadataTarget = extractCooFromMetadata(metadataStream);
            // combine with use case joboe and jobposition
            return new DmsTarget(metadataTarget.coo(), metadataTarget.userName(), useCase.getJoboe(), useCase.getJobposition());
        } catch (final IOException e) {
            throw new MetadataException("Error while processing metadata file", e);
        }
    }

    /**
     * Resolve use case via name.
     *
     * @param useCase The name of the use case.
     * @return The use case with the name.
     * @throws UnknownUseCaseException If there is no use case with that name.
     */
    protected UseCase findUseCase(@NotBlank final String useCase) {
        return swimDmsProperties.getUseCases().stream()
                .filter(i -> i.getName().equals(useCase)).findFirst()
                .orElseThrow(() -> new UnknownUseCaseException(String.format("Unknown use case %s", useCase)));
    }

    /**
     * Extract inbox coo from metadat file.
     *
     * @param inputStream InputStream of metadata file.
     * @return The inbox coo.
     * @throws MetadataException If file can't be parsed or required values are missing.
     */
    protected DmsTarget extractCooFromMetadata(@NotNull final InputStream inputStream) {
        try {
            final JsonNode rootNode = objectMapper.readTree(inputStream);
            final JsonNode indexFieldsNode = rootNode.get(METADATA_INDEX_FIELDS_KEY);
            String cooAddress = null;
            String username = null;
            for (final JsonNode indexField : indexFieldsNode) {
                if (METADATA_INBOX_COO_KEY.equals(indexField.path("Name").asText())) {
                    cooAddress = indexField.path("Value").asText();
                } else if (METADATA_USERNAME_KEY.equals(indexField.path("Name").asText())) {
                    username = indexField.path("Value").asText();
                }
            }
            if (cooAddress == null || username == null) {
                throw new MetadataException("Coo or username not found in metadata json");
            }
            return new DmsTarget(cooAddress, username, null, null);
        } catch (final IOException e) {
            throw new MetadataException("Error while parsing metadata json", e);
        }
    }

    /**
     * Apply pattern to input String by joining all matching groups.
     *
     * @param pattern Pattern to apply.
     * @param input Input to apply pattern to.
     * @param joiner Sequence for joining matching groups.
     * @return Result string.
     */
    protected String applyOverwritePattern(final String pattern, final String input, final String joiner) {
        if (Strings.isBlank(pattern)) {
            return input;
        }
        final Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.find()) {
            final List<String> groups = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            return String.join(joiner, groups);
        } else {
            throw new IllegalStateException("Overwrite pattern not matching input");
        }
    }
}

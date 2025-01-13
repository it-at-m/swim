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
            // transfer to dms
            final String filename = String.format("%s", file.path().substring(file.path().lastIndexOf('/') + 1));
            switch (useCase.getType()) {
            // to dms inbox
            case INBOX -> dmsOutPort.putFileInInbox(dmsTarget, filename, fileStream);
            // create dms incoming
            // TODO incoming name
            case INCOMING_OBJECT -> dmsOutPort.createIncoming(dmsTarget, filename, filename, fileStream);
            }
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(useCaseName, presignedUrl, metadataPresignedUrl);
    }

    /**
     * Resolve target coo for useCase. Either via metadata file or filename.
     *
     * @param metadataPresignedUrl Presigned url for metadata file.
     * @param useCase The use case.
     * @return The resolved coo.
     */
    protected DmsTarget resolveTargetCoo(final String metadataPresignedUrl, final UseCase useCase, final File file) {
        return switch (useCase.getCooSource()) {
        // resolve coo from metadata file
        case METADATA_FILE -> {
            // validate metadata presigned url provided
            if (Strings.isBlank(metadataPresignedUrl)) {
                throw new MetadataException("Metadata presigned url empty but required");
            }
            // get metadata file
            try (InputStream metadataStream = fileSystemOutPort.getPresignedUrlFile(metadataPresignedUrl)) {
                // extract coo
                yield extractCooFromMetadata(metadataStream);
            } catch (final IOException e) {
                throw new MetadataException("Error while processing metadata file", e);
            }
        }
        // TODO resolve coo from filename
        case FILENAME -> throw new UnsupportedOperationException("Coo source type filename not implemented yet");
        case STATIC -> new DmsTarget(useCase.getTargetCoo(), useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        };
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
}
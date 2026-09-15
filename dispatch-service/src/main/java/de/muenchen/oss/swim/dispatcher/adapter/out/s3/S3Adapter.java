package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileMetadata;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import de.muenchen.oss.refarch.integration.s3.domain.model.PresignedUrl;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileNotFoundException;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSystemAccessException;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;

@Service
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class S3Adapter implements FileSystemOutPort, ReadProtocolOutPort {
    private static final char PROTOCOL_DELIMITER = '|';
    private static final int PROTOCOL_SKIP_ROWS = 1;

    private final S3OutPort s3OutPort;
    private final ProtocolMapper protocolMapper;
    private final S3Properties s3Properties;
    private final SwimDispatcherProperties swimDispatcherProperties;

    /* default */ S3Adapter(final S3OutPort s3OutPort, final S3Properties s3Properties, final ProtocolMapper protocolMapper,
            final SwimDispatcherProperties swimDispatcherProperties) {
        this.s3OutPort = s3OutPort;
        this.protocolMapper = protocolMapper;
        this.s3Properties = s3Properties;
        this.swimDispatcherProperties = swimDispatcherProperties;
    }

    @Override
    public List<FileWithMetadata> getMatchingFilesWithTags(
            final String bucket,
            final String pathPrefix,
            final boolean recursive,
            final String extension,
            final Map<String, String> requiredTags,
            final Map<String, List<String>> excludeTags) {
        final String suffix = String.format(".%s", extension);
        return getObjectsInPath(bucket, pathPrefix, recursive).stream()
                // filter out dirs
                .filter(i -> !i.path().endsWith("/"))
                // filter extension
                .filter(i -> i.path().toLowerCase(Locale.ROOT).endsWith(suffix))
                // load tags of each reference and map
                .map(i -> {
                    Map<String, String> tags = null;
                    try {
                        tags = getTagsOfFile(new FileReference(bucket, i.path()));
                    } catch (final FileNotFoundException ignored) {
                        // could occur if reference was moved between getObjectsInPath and this tag load
                        log.trace("FileReference not found while getting tags for reference list: {} in {}", i.path(), bucket);
                    }
                    return new FileWithMetadata(new FileReference(bucket, i.path()), i.contentLength(),
                            ZonedDateTime.ofInstant(i.lastModified(), ZoneId.systemDefault()), tags);
                })
                // filter tags
                .filter(i -> {
                    // check if tags could be loaded and matching required and exclude
                    return i.tags() != null && matchesMap(i.tags(), requiredTags, excludeTags);
                }).toList();
    }

    @Override
    public List<String> getSubDirectories(final String bucket, final String pathPrefix) {
        // ensure prefix is handled as specific dir
        final String escapedPathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        // build s3 list request
        try {
            return this.s3OutPort.getFilesWithPrefix(bucket, escapedPathPrefix, false).commonPrefixes();
        } catch (final S3Exception e) {
            final String message = String.format("Error while listing s3 directories for bucket %s in path %s", bucket, pathPrefix);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public void tagFile(final FileReference fileReference, final Map<String, String> tags) {
        try {
            // get current tags
            final Map<String, String> currentTags = getTagsOfFile(fileReference);
            // clear errors
            currentTags.remove(swimDispatcherProperties.getErrorClassTagKey());
            currentTags.remove(swimDispatcherProperties.getErrorMessageTagKey());
            // build new tags
            final Map<String, String> newTags = new HashMap<>(currentTags);
            newTags.putAll(tags);
            // build request
            this.s3OutPort.setTags(toS3FileReference(fileReference), newTags);
        } catch (final S3Exception | FileNotFoundException e) {
            final String message = String.format("Error while tagging s3 file %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public boolean fileExists(final FileReference fileReference) {
        try {
            return this.s3OutPort.fileExists(toS3FileReference(fileReference));
        } catch (final S3Exception e) {
            final String message = String.format("Error while getting s3 file %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public InputStream readFile(final FileReference fileReference) {
        try {
            return this.s3OutPort.getFileContent(toS3FileReference(fileReference));
        } catch (final S3Exception e) {
            final String message = String.format("Error while downloading file %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public String getPresignedUrl(final FileReference fileReference) {
        try {
            final PresignedUrl presignedUrl = this.s3OutPort.getPresignedUrl(
                    toS3FileReference(fileReference), PresignedUrl.Action.GET, s3Properties.getPresignedUrlExpiry());
            return presignedUrl.url().toString();
        } catch (final S3Exception e) {
            final String message = String.format("Error while getting presigned url for file %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    @SuppressFBWarnings("URLCONNECTION_SSRF_FD")
    public boolean verifyPresignedUrl(final String presignedUrl) throws PresignedUrlException {
        try {
            final URI uri = new URI(presignedUrl);
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            // Request only the first byte to only get headers
            connection.setRequestProperty("Range", "bytes=0-0");

            final HttpStatusCode responseCode = HttpStatusCode.valueOf(connection.getResponseCode());
            return responseCode.is2xxSuccessful();
        } catch (final IOException | URISyntaxException e) {
            throw new PresignedUrlException("Presigned url verification failed", e);
        }
    }

    @Override
    public void moveFile(final FileReference srcFileReference, final String destPath) {
        // copy file
        this.copyFile(srcFileReference, new FileReference(srcFileReference.bucket(), destPath), false);
        // delete src file
        this.deleteFile(srcFileReference);
    }

    @Override
    public void copyFile(final FileReference srcFileReference, final FileReference destFileReference, final boolean clearTags) {
        try {
            this.s3OutPort.copyFile(toS3FileReference(srcFileReference), toS3FileReference(destFileReference), !clearTags);
            log.info("Copied file {} to {}", srcFileReference, destFileReference);
        } catch (final S3Exception e) {
            final String message = String.format("Error while copying s3 object %s to %s",
                    srcFileReference, destFileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    /**
     * Delete a file.
     *
     * @param fileReference The reference of the file.
     */
    protected void deleteFile(final FileReference fileReference) {
        try {
            this.s3OutPort.deleteFile(toS3FileReference(fileReference));
            log.info("Deleted file {}", fileReference);
        } catch (final S3Exception e) {
            final String message = String.format("Error while deleting s3 object %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    /**
     * Get objects (dirs/files) in a specific bucket and path.
     *
     * @param bucket Bucket to look in.
     * @param pathPrefix Path prefix to look in.
     * @param recursive If searching recursive or only direct in the path.
     * @return Objects in the path.
     */
    protected List<FileMetadata> getObjectsInPath(final String bucket, final String pathPrefix, final boolean recursive) {
        // ensure prefix is handled as specific dir
        final String escapedPathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        try {
            final ListResult listResult = this.s3OutPort.getFilesWithPrefix(bucket, escapedPathPrefix, recursive);
            return listResult.files();
        } catch (final S3Exception e) {
            final String message = String.format("Error while listing s3 objects for bucket %s in path %s", bucket, pathPrefix);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    /**
     * Return tags of a specific reference.
     *
     * @param fileReference The reference of the file.
     * @return Tags the file has.
     * @throws FileNotFoundException If key can't be found in S3.
     */
    protected Map<String, String> getTagsOfFile(final FileReference fileReference) throws FileNotFoundException {
        try {
            return this.s3OutPort.getTags(toS3FileReference(fileReference));
        } catch (final S3Exception e) {
            if (e.getCause() instanceof software.amazon.awssdk.services.s3.model.S3Exception s3Exception
                    && s3Exception.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                final String message = String.format("File %s can't be found", fileReference);
                throw new FileNotFoundException(message, e);
            }
            final String message = String.format("Error while getting tags for s3 file %s", fileReference);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    private de.muenchen.oss.refarch.integration.s3.domain.model.FileReference toS3FileReference(final FileReference fileReference) {
        return new de.muenchen.oss.refarch.integration.s3.domain.model.FileReference(fileReference.bucket(), fileReference.path());
    }

    /**
     * Check if a map contains all entries from another one and none from another one.
     *
     * @param base The map to check if fulfilling requirements.
     * @param requiredEntries The map of required entries.
     * @param excludeEntries The map of excluded entries.
     * @return If the map fulfills the required and exclude maps.
     */
    protected boolean matchesMap(final Map<String, String> base, final Map<String, String> requiredEntries, final Map<String, List<String>> excludeEntries) {
        // Check if map contains all required key-value pairs
        for (final Map.Entry<String, String> requiredEntry : requiredEntries.entrySet()) {
            final String key = requiredEntry.getKey();
            final String value = requiredEntry.getValue();
            if (!base.containsKey(key) || !base.get(key).equals(value)) {
                return false; // Missing a required key-value pair
            }
        }

        // Check if map contains any excluded key-value pairs
        for (final Map.Entry<String, List<String>> excludeEntry : excludeEntries.entrySet()) {
            final String key = excludeEntry.getKey();
            final List<String> value = excludeEntry.getValue();
            if (base.containsKey(key) && value.contains(base.get(key))) {
                return false; // Found an excluded key-value pair
            }
        }

        return true;
    }

    @Override
    public List<ProtocolEntry> loadProtocol(final FileReference fileReference) {
        // build csv schema
        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.typedSchemaFor(CsvProtocolEntry.class)
                .withHeader()
                .withColumnSeparator(PROTOCOL_DELIMITER)
                .withColumnReordering(true);
        // parse csv
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.readFile(fileReference), StandardCharsets.UTF_8))) {
            // skip n rows
            for (int i = 0; i < PROTOCOL_SKIP_ROWS; i++) {
                reader.readLine();
            }
            // parse csv
            try (MappingIterator<CsvProtocolEntry> iterator = csvMapper
                    .readerFor(CsvProtocolEntry.class)
                    .with(schema)
                    .with(CsvReadFeature.SKIP_EMPTY_LINES)
                    .without(CsvReadFeature.FAIL_ON_MISSING_HEADER_COLUMNS)
                    .readValues(reader)) {
                return protocolMapper.toDomain(iterator.readAll());
            }
        } catch (JacksonException | IOException e) {
            final String message = String.format("Error while parsing protocol from file %s", fileReference);
            throw new ProtocolException(message, e);
        }
    }
}

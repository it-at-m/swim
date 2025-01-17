package de.muenchen.swim.dispatcher.adapter.out.s3;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.swim.dispatcher.domain.exception.FileSystemAccessException;
import de.muenchen.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.swim.dispatcher.domain.model.File;
import de.muenchen.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.GetObjectArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.SetObjectTagsArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Item;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class S3Adapter implements FileSystemOutPort, ReadProtocolOutPort {
    /**
     * Response code from S3 storage when an object cannot be found.
     */
    private static final String ERROR_CODE_NO_SUCH_KEY = "NoSuchKey";
    private static final char PROTOCOL_DELIMITER = '|';
    private static final int PROTOCOL_SKIP_ROWS = 1;

    private final MinioClient minioClient;
    private final ProtocolMapper protocolMapper;
    private final S3Properties s3Properties;

    /* default */ S3Adapter(@Autowired final S3Properties s3Properties, @Autowired final ProtocolMapper protocolMapper) {
        this.protocolMapper = protocolMapper;
        this.s3Properties = s3Properties;
        this.minioClient = MinioClient.builder()
                .endpoint(s3Properties.getUrl())
                .credentials(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                .build();
    }

    @Override
    public List<File> getMatchingFiles(
            final String bucket,
            final String pathPrefix,
            final boolean recursive,
            final String extension,
            final Map<String, String> requiredTags,
            final Map<String, List<String>> excludeTags) {
        final String suffix = String.format(".%s", extension);
        return getFilesInPath(bucket, pathPrefix, recursive).stream()
                // filter extension
                .filter(i -> i.path().toLowerCase(Locale.ROOT).endsWith(suffix))
                // filter tags
                .filter(i -> {
                    // get file tags
                    final Map<String, String> tags = getTagsOfFile(bucket, i.path());
                    // check if matching required and exclude
                    return matchesMap(tags, requiredTags, excludeTags);
                })
                .toList();
    }

    @Override
    public void tagFile(final String bucket, final String path, final Map<String, String> tags) {
        // get current tags
        final Map<String, String> currentTags = getTagsOfFile(bucket, path);
        // build new tags
        final Map<String, String> newTags = new HashMap<>(currentTags);
        newTags.putAll(tags);
        // build request
        final SetObjectTagsArgs setObjectTagsArgs = SetObjectTagsArgs.builder()
                .bucket(bucket)
                .object(path)
                .tags(newTags)
                .build();
        try {
            // set tags
            minioClient.setObjectTags(setObjectTagsArgs);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while tagging s3 file for bucket %s in path %s", bucket, path);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public boolean fileExists(final String bucket, final String path) {
        final StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build();
        try {
            minioClient.statObject(statObjectArgs);
            return true;
        } catch (final ErrorResponseException e) {
            // handle exception which indicates file doesn't exist
            if (ERROR_CODE_NO_SUCH_KEY.equals(e.errorResponse().code())) {
                return false;
            } else {
                final String message = String.format("ErrorResponseException while getting s3 file for bucket %s in path %s", bucket, path);
                log.error(message, e);
                throw new FileSystemAccessException(message, e);
            }
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while getting s3 file for bucket %s in path %s", bucket, path);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public InputStream readFile(final String bucket, final String path) {
        final GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build();
        try {
            return minioClient.getObject(getObjectArgs);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while downloading file %s from bucket %s", path, bucket);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    public String getPresignedUrl(final String bucket, final String path) {
        final GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(path)
                .method(Method.GET)
                .expiry(s3Properties.getPresignedUrlExpiry())
                .build();
        try {
            return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while getting presigned url for bucket %s in path %s", bucket, path);
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

    /**
     * Get files in a specific bucket and path.
     *
     * @param bucket Bucket to look in.
     * @param pathPrefix Path prefix to look in.
     * @param recursive If searching recursive or only direct in the path.
     * @return Files in the path.
     */
    protected List<File> getFilesInPath(final String bucket, final String pathPrefix, final boolean recursive) {
        // ensure prefix is handled as specific dir
        final String escapedPathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        // build s3 list request
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(escapedPathPrefix)
                .recursive(recursive).build();
        // list objects
        final List<Result<Item>> listResult = IteratorUtils.toList(minioClient.listObjects(listObjectsArgs).iterator());
        try {
            final List<File> files = new ArrayList<>();
            for (final Result<Item> resultItem : listResult) {
                files.add(new File(bucket, resultItem.get().objectName(), resultItem.get().size()));
            }
            return files;
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while listing s3 files for bucket %s in path %s", bucket, pathPrefix);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    /**
     * Return tags of a specific file.
     *
     * @param bucket Bucket in which the file is in.
     * @param objectName Name of the file.
     * @return Tags the file has.
     */
    protected Map<String, String> getTagsOfFile(final String bucket, final String objectName) {
        final GetObjectTagsArgs getObjectTagsArgs = GetObjectTagsArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build();
        try {
            return minioClient.getObjectTags(getObjectTagsArgs).get();
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while getting tags for s3 file %s in bucket %s", objectName, bucket);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
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
    public List<ProtocolEntry> loadProtocol(final String bucket, final String path) {
        // build csv schema
        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.typedSchemaFor(CsvProtocolEntry.class)
                .withHeader()
                .withColumnSeparator(PROTOCOL_DELIMITER)
                .withColumnReordering(true);
        // parse csv
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.readFile(bucket, path), StandardCharsets.UTF_8))) {
            // skip n rows
            for (int i = 0; i < PROTOCOL_SKIP_ROWS; i++) {
                reader.readLine();
            }
            // parse csv
            try (MappingIterator<CsvProtocolEntry> iterator = csvMapper
                    .readerFor(CsvProtocolEntry.class)
                    .with(schema)
                    .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                    .readValues(reader)) {
                return protocolMapper.toDomain(iterator.readAll());
            }
        } catch (IOException e) {
            final String message = String.format("Error while parsing protocol %s in bucket %s", path, bucket);
            throw new ProtocolException(message, e);
        }
    }
}

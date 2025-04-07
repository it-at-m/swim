package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileNotFoundException;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSystemAccessException;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.Directive;
import io.minio.GetObjectArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.GodClass" })
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
    private final SwimDispatcherProperties swimDispatcherProperties;

    /* default */ S3Adapter(@Autowired final S3Properties s3Properties, @Autowired final ProtocolMapper protocolMapper,
            @Autowired final SwimDispatcherProperties swimDispatcherProperties) {
        this.protocolMapper = protocolMapper;
        this.s3Properties = s3Properties;
        this.minioClient = MinioClient.builder()
                .endpoint(s3Properties.getUrl())
                .credentials(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                .build();
        this.swimDispatcherProperties = swimDispatcherProperties;
    }

    @Override
    public Map<File, Map<String, String>> getMatchingFilesWithTags(
            final String bucket,
            final String pathPrefix,
            final boolean recursive,
            final String extension,
            final Map<String, String> requiredTags,
            final Map<String, List<String>> excludeTags) {
        final String suffix = String.format(".%s", extension);
        return getObjectsInPath(bucket, pathPrefix, recursive).stream()
                // filter out dirs
                .filter(i -> !i.isDir())
                // map to file
                .map(i -> new File(bucket, i.objectName(), i.size()))
                // filter extension
                .filter(i -> i.path().toLowerCase(Locale.ROOT).endsWith(suffix))
                // map tags to each file
                .map(i -> {
                    Map<String, String> tags = null;
                    try {
                        tags = getTagsOfFile(bucket, i.path());
                    } catch (final FileNotFoundException ignored) {
                        // could occur if file was moved between getObjectsInPath and this tag load
                        log.trace("File not found while getting tags for file list: {} in {}", i.path(), i.bucket());
                    }
                    return new AbstractMap.SimpleEntry<>(i, tags);
                })
                // filter tags
                .filter((entry) -> {
                    // check if tags could be loaded and matching required and exclude
                    return entry.getValue() != null && matchesMap(entry.getValue(), requiredTags, excludeTags);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<String> getSubDirectories(final String bucket, final String pathPrefix) {
        // ensure prefix is handled as specific dir
        final String escapedPathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        // build s3 list request
        return this.getObjectsInPath(bucket, escapedPathPrefix, false).stream()
                .filter(Item::isDir)
                .map(Item::objectName).toList();
    }

    @Override
    public void tagFile(final String bucket, final String path, final Map<String, String> tags) {
        try {
            // get current tags
            final Map<String, String> currentTags = getTagsOfFile(bucket, path);
            // clear errors
            currentTags.remove(swimDispatcherProperties.getErrorClassTagKey());
            currentTags.remove(swimDispatcherProperties.getErrorMessageTagKey());
            // build new tags
            final Map<String, String> newTags = new HashMap<>(currentTags);
            newTags.putAll(tags);
            // build request
            final SetObjectTagsArgs setObjectTagsArgs = SetObjectTagsArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .tags(newTags)
                    .build();
            // set tags
            minioClient.setObjectTags(setObjectTagsArgs);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException | FileNotFoundException e) {
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

    @Override
    public void moveFile(final String bucket, final String srcPath, final String destPath) {
        try {
            // copy file
            final CopySource copySource = CopySource.builder()
                    .bucket(bucket)
                    .object(srcPath)
                    .build();
            final CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                    .bucket(bucket)
                    .source(copySource)
                    .object(destPath).build();
            this.minioClient.copyObject(copyObjectArgs);
            // delete file
            final RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucket).object(srcPath).build();
            this.minioClient.removeObject(removeObjectArgs);
            log.info("Moved file in bucket {} from {} to {}", bucket, srcPath, destPath);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while moving s3 object for bucket %s from path %s to %s", bucket, srcPath, destPath);
            log.error(message, e);
            throw new FileSystemAccessException(message, e);
        }
    }

    @Override
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public void copyFile(final String srcBucket, final String srcPath, final String destBucket, final String destPath, final boolean clearTags) {
        try {
            final CopySource copySource = CopySource.builder()
                    .bucket(srcBucket)
                    .object(srcPath)
                    .build();
            final CopyObjectArgs.Builder copyObjectArgs = CopyObjectArgs.builder()
                    .bucket(destBucket)
                    .source(copySource)
                    .object(destPath);
            if (clearTags) {
                copyObjectArgs.taggingDirective(Directive.REPLACE);
                // workaround as empty tags are not set as header by minio but required
                copyObjectArgs.extraHeaders(Map.of(
                        "x-amz-tagging", ""));
            }
            this.minioClient.copyObject(copyObjectArgs.build());
            log.info("Copied file {} from bucket {} to {} in bucket {}", srcPath, srcBucket, destPath, destBucket);
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while copying s3 object %s from bucket %s to %s in bucket %s", srcPath, srcBucket, destPath,
                    destBucket);
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
    protected List<Item> getObjectsInPath(final String bucket, final String pathPrefix, final boolean recursive) {
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
            final List<Item> objects = new ArrayList<>();
            for (final Result<Item> resultItem : listResult) {
                objects.add(resultItem.get());
            }
            return objects;
        } catch (final MinioException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
            final String message = String.format("Error while listing s3 objects for bucket %s in path %s", bucket, pathPrefix);
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
     * @throws FileNotFoundException If key can't be found in S3.
     */
    protected Map<String, String> getTagsOfFile(final String bucket, final String objectName) throws FileNotFoundException {
        final GetObjectTagsArgs getObjectTagsArgs = GetObjectTagsArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build();
        try {
            final Map<String, String> tags = minioClient.getObjectTags(getObjectTagsArgs).get();
            // workaround: convert null value to empty string
            return tags.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() == null ? "" : entry.getValue()));
        } catch (final ErrorResponseException e) {
            // handle exception which indicates file doesn't exist
            if (ERROR_CODE_NO_SUCH_KEY.equals(e.errorResponse().code())) {
                final String message = String.format("File %s can't be found in bucket %s", objectName, bucket);
                throw new FileNotFoundException(message, e);
            } else {
                final String message = String.format("ErrorResponseException while getting tags for s3 file %s in bucket %s", objectName, bucket);
                log.error(message, e);
                throw new FileSystemAccessException(message, e);
            }
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
                    .without(CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS)
                    .readValues(reader)) {
                return protocolMapper.toDomain(iterator.readAll());
            }
        } catch (IOException e) {
            final String message = String.format("Error while parsing protocol %s in bucket %s", path, bucket);
            throw new ProtocolException(message, e);
        }
    }
}

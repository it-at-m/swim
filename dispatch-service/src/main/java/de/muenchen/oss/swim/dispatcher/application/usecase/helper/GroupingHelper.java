package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Helper for grouping and validating files that are stored in numbered chunks.
 * <p>
 * A chunked file is identified by a specific pattern in its filename (without extension):
 * <code>&lt;base&gt;_[index]v[count]</code> or <code>&lt;base&gt;-[index]v[count]</code>.
 * Examples: <code>report-1v3</code>, <code>report_2v3</code>, <code>video-10v10</code>.
 * </p>
 * <p>
 */
@Component
@RequiredArgsConstructor
public class GroupingHelper {
    public final static Pattern CHUNKED_FILE_PATTERN = Pattern.compile("(.+)[-_](\\d+)v(\\d+)$");
    public final static int CHUNKED_FILE_BASE_NAME_GROUP = 1;
    public final static int CHUNKED_FILE_INDEX_GROUP = 2;
    public final static int CHUNKED_FILE_COUNT_GROUP = 3;

    private final SwimDispatcherProperties swimDispatcherProperties;

    /**
     * Groups a list of files by their base filename.
     * <p>
     * For filenames matching the chunk pattern, the returned map will contain an entry keyed by the
     * base name
     * with a list of all chunks belonging to that base. Filenames that do not match the pattern are
     * grouped
     * by their full filename (without extension) as a single-element list.
     *
     * @param files flat list of files (chunked and non-chunked)
     * @return map from base filename to the corresponding list of files
     */
    public Map<String, FileGroup> groupFiles(final List<FileWithMetadata> files) {
        final Map<String, FileGroup> groupedFiles = new HashMap<>();
        for (final FileWithMetadata file : files) {
            final Matcher matcher = CHUNKED_FILE_PATTERN.matcher(file.reference().getFileNameWithoutExtension());
            if (matcher.matches()) {
                final String originalFileName = matcher.group(CHUNKED_FILE_BASE_NAME_GROUP);
                if (groupedFiles.containsKey(originalFileName)) {
                    final FileGroup fileGroup = groupedFiles.get(originalFileName);
                    if (!fileGroup.isMulti()) {
                        throw new IllegalStateException("Is multi file pattern but group isn't multi");
                    }
                    fileGroup.add(file);
                } else {
                    groupedFiles.put(originalFileName, new FileGroup(true, file));
                }
            } else {
                groupedFiles.put(file.reference().getFileNameWithoutExtension(), new FileGroup(false, file));
            }
        }
        return groupedFiles;
    }

}

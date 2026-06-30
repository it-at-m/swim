package de.muenchen.oss.swim.dispatcher.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class FileGroup {
    private final boolean isMulti;
    private List<FileWithMetadata> files = new ArrayList<>();

    public FileGroup(final boolean isMulti, final FileWithMetadata fileWithMetadata) {
        this.isMulti = isMulti;
        files.add(fileWithMetadata);
    }

    public FileGroup(final boolean isMulti, final List<FileWithMetadata> files) {
        this.isMulti = isMulti;
        this.files = files;
    }

    public void add(final FileWithMetadata file) {
        files.add(file);
    }
}

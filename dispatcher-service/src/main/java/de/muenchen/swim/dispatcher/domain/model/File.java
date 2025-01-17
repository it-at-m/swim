package de.muenchen.swim.dispatcher.domain.model;

import jakarta.validation.constraints.NotBlank;

public record File(@NotBlank String bucket, @NotBlank String path, Long size) {
    public String getFileName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String getFileNameWithoutExtension() {
        final String fileName = this.getFileName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public String getParentPath() {
        return path.substring(0, path.lastIndexOf('/'));
    }

    public String getParentName() {
        final String parentPath = this.getParentPath();
        return parentPath.substring(parentPath.lastIndexOf('/') + 1);
    }
}

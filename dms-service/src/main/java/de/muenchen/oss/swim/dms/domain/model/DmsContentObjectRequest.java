package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Properties for creating a new ContentObject.
 *
 * @param name The name of the new ContentObject including the file extension.
 * @param subject The subject of the new ContentObject.
 */
public record DmsContentObjectRequest(@NotBlank String name, String subject) {
    /**
     * Get the name without the file extension.
     *
     * @return The name without the file extension.
     */
    public String getNameWithoutExtension() {
        return name.substring(0, name.lastIndexOf('.'));
    }
}

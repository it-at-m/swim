package de.muenchen.oss.swim.dipa.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Context for a DiPa request.
 */
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class DipaRequestContext {
    /**
     * User under which the request is executed.
     */
    @NotBlank
    private final String username;
}

package de.muenchen.oss.swim.dipa.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UseCase {
    @NotBlank
    private String name;
    /**
     * Type of the use case.
     * Specifies where the file is put.
     */
    @NotNull
    private UseCaseType type;
    /**
     * Defines how the target coo is gathered.
     */
    @NotNull
    private UseCaseSource cooSource;
    /**
     * Properties for creating an Incoming.
     * Applies to {@link UseCaseType#getType()} being {@link DipaResourceType#INCOMING}.
     */
    @NotNull
    private UseCaseIncoming incoming = new UseCaseIncoming();
    /**
     * Properties for creating a ContentObject.
     * Applies to {@link UseCaseType#getType()} being {@link DipaResourceType#INCOMING}.
     */
    @NotNull
    private UseCaseContentObject contentObject = new UseCaseContentObject();
    /**
     * Context under which DiPa requests are made.
     */
    @NotNull
    private DipaRequestContext context = new DipaRequestContext(null);
}

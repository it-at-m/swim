package de.muenchen.oss.swim.dipa.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
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
     * Defines how the target is gathered.
     */
    @NotNull
    @Valid
    private UseCaseSource targetSource;
    /**
     * Properties for creating an Incoming.
     * Applies to {@link UseCaseType#getType()} being {@link DipaResourceType#INCOMING}.
     */
    @NotNull
    @Valid
    private UseCaseIncoming incoming = new UseCaseIncoming();
    /**
     * Properties for creating a ContentObject.
     * Applies to {@link UseCaseType#getType()} being {@link DipaResourceType#INCOMING}.
     */
    @NotNull
    @Valid
    private UseCaseContentObject contentObject = new UseCaseContentObject();
    /**
     * Context under which DiPa requests are made.
     */
    @NotNull
    @Valid
    private DipaRequestContext context;
}

package de.muenchen.oss.swim.invoice.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UseCase {
    @NotBlank
    private String name;
}

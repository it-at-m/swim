package de.muenchen.oss.swim.matching.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class GroupDmsInbox {
    @NotBlank
    @Pattern(regexp = "COO[\\d.]+]")
    private final String coo;
    @NotBlank
    private final String name;
    @NotBlank
    private final String ou;
    @NotBlank
    private final String dmsTenant;
}

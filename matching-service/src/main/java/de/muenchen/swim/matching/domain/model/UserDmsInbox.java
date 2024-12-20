package de.muenchen.swim.matching.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class UserDmsInbox {
    @NotBlank
    @Pattern(regexp = "COO[\\d.]+]")
    private final String coo;
    @NotBlank
    private final String name;
    @NotBlank
    private final String ownerLhmObjectId;
    @NotBlank
    private final String ou;
    @NotBlank
    private final String dmsTenant;
    @NotNull
    private final User user;
}

package de.muenchen.oss.swim.matching.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class DmsInbox {
    @NotBlank
    @Pattern(regexp = "COO[\\d.]+")
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
    private final InboxType type;

    public enum InboxType {
        USER,
        GROUP
    }
}

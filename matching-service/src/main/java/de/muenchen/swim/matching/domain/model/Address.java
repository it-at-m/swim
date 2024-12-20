package de.muenchen.swim.matching.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Address {
    private final String street;
    private final String postalcode;
    private final String city;
}

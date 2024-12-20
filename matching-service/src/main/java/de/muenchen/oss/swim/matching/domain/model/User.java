package de.muenchen.oss.swim.matching.domain.model;

public record User(
        String lhmObjectId,
        String username,
        String firstname,
        String surname,
        String ou,
        Address officeAddress,
        Address postalAddress) {
}

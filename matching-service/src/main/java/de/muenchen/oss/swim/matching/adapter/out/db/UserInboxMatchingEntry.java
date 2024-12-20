package de.muenchen.oss.swim.matching.adapter.out.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = UserInboxMatchingEntry.TABLE_NAME)
class UserInboxMatchingEntry {
    public static final String TABLE_NAME = "lhm_user_inbox";
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;
    @NotBlank
    private String coo;
    @NotBlank
    private String inboxName;
    private String username;
    private String firstname;
    private String surname;
    private String ou;
    private String street;
    private String postalCode;
    private String city;
    @NotBlank
    private String dmsTenant;
}

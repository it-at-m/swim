package de.muenchen.oss.swim.matching.adapter.out.db;

import jakarta.persistence.Entity;
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
@Entity(name = GroupInboxMatchingEntry.TABLE_NAME)
class GroupInboxMatchingEntry {
    public static final String TABLE_NAME = "lhm_group_inbox";
    @Id
    private String coo;
    @NotBlank
    private String inboxName;
    @NotBlank
    private String ou;
    @NotBlank
    private String dmsTenant;
}

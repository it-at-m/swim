package de.muenchen.oss.swim.matching.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import lombok.Data;

@Data
@SuppressFBWarnings("EI_EXPOSE_REP")
public class ImportReport {
    /**
     * Count of inboxes in input file.
     */
    private int inputInboxes;
    /**
     * List of all tenants present in input file and replaced by import.
     */
    private Set<String> dmsTenants;
    /**
     * Count of user inboxes in input file.
     */
    private int userInboxes;
    /**
     * Count of user inboxes which where successfully imported.
     */
    private int importedUserInboxes;
    /**
     * Count of user inboxes for which no ldap user could be resolved and which where not imported.
     */
    private int unresolvableUserInboxes;
    /**
     * Count of group inboxes in input file.
     */
    private int groupInboxes;
    /**
     * Count of group inboxes for which no ldap user could be resolved and which where not imported.
     */
    private int unresolvableGroupInboxes;
    /**
     * Count of group inboxes which where successfully imported.
     */
    private int importedGroupInboxes;
}

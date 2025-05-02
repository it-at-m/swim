package de.muenchen.oss.swim.matching.domain.model;

import de.muenchen.oss.swim.matching.configuration.SwimMatchingProperties;
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
     * Count of inboxes after filtering.
     * See {@link SwimMatchingProperties#getDmsTenants()}
     */
    private int filteredInboxes;
    /**
     * List of all tenants present in input file and replaced by import.
     */
    private Set<String> dmsTenants;
    /**
     * Import state for user inboxes;
     */
    private InboxState userInboxes = new ImportReport.InboxState();
    /**
     * Import state for group inboxes;
     */
    private InboxState groupInboxes = new ImportReport.InboxState();

    @Data
    public static class InboxState {
        /**
         * Count of inboxes input for processing.
         * Out of {@link ImportReport#filteredInboxes}.
         */
        private int input;
        /**
         * Count of inboxes imported.
         */
        private int imported;
        /**
         * Count of inboxes for which no ldap user could be resolved and which where not imported.
         */
        private int unresolvable;
    }
}

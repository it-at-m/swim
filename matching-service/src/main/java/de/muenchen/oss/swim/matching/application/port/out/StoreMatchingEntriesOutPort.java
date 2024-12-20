package de.muenchen.oss.swim.matching.application.port.out;

import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface StoreMatchingEntriesOutPort {
    /**
     * Store user inboxes into matching datasource.
     *
     * @param userInboxes The user inboxes to store.
     */
    void storeUserInboxes(@NotNull List<UserDmsInbox> userInboxes);

    /**
     * Store group inboxes into matching datasource.
     *
     * @param groupInboxes The group inboxes to store.
     */
    void storeGroupInboxes(@NotNull List<GroupDmsInbox> groupInboxes);
}

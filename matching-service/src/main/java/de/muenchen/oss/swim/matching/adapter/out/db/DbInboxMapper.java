package de.muenchen.oss.swim.matching.adapter.out.db;

import de.muenchen.oss.swim.matching.domain.model.Address;
import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface DbInboxMapper {
    /**
     * Converts a single {@link UserDmsInbox} into all its {@link UserInboxMatchingEntry}s.
     * For each unique address a user has a separate matching entry is required.
     *
     * @param userDmsInbox The inbox to create the matching entries from.
     * @return List of unique matching entries.
     */
    static List<UserInboxMatchingEntry> toUserInboxMatchingEntries(final UserDmsInbox userDmsInbox) {
        final List<UserInboxMatchingEntry> userInboxMatchingEntries = new ArrayList<>();
        // add office address
        userInboxMatchingEntries.add(toUserInboxMatchingEntry(userDmsInbox, userDmsInbox.getUser().officeAddress()));
        // add postal address only if different
        if (!userDmsInbox.getUser().officeAddress().equals(userDmsInbox.getUser().postalAddress())) {
            userInboxMatchingEntries.add(toUserInboxMatchingEntry(userDmsInbox, userDmsInbox.getUser().postalAddress()));
        }
        return userInboxMatchingEntries;
    }

    /**
     * Creates a single {@link UserInboxMatchingEntry} for provided {@link UserDmsInbox} and
     * {@link Address}.
     *
     * @param userDmsInbox The userDmsInbox to create the matching entry from.
     * @param address The address to create the matching entry from.
     * @return The according matching entry.
     */
    static UserInboxMatchingEntry toUserInboxMatchingEntry(final UserDmsInbox userDmsInbox, final Address address) {
        return new UserInboxMatchingEntry(
                null,
                userDmsInbox.getCoo(),
                userDmsInbox.getName(),
                userDmsInbox.getUser().username(),
                userDmsInbox.getUser().firstname(),
                userDmsInbox.getUser().surname(),
                userDmsInbox.getUser().ou(),
                address.getStreet(),
                address.getPostalcode(),
                address.getCity(),
                userDmsInbox.getDmsTenant());
    }

    static List<UserInboxMatchingEntry> toUserInboxes(final List<UserDmsInbox> userDmsInboxes) {
        return userDmsInboxes.stream()
                .map(DbInboxMapper::toUserInboxMatchingEntries)
                .flatMap(List::stream).toList();
    }

    @Mapping(source = "name", target = "inboxName")
    GroupInboxMatchingEntry toDbGroupInbox(GroupDmsInbox groupDmsInbox);

    List<GroupInboxMatchingEntry> toDbGroupInboxes(List<GroupDmsInbox> inboxes);
}

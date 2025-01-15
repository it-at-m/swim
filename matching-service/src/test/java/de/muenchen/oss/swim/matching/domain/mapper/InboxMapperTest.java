package de.muenchen.oss.swim.matching.domain.mapper;

import static de.muenchen.oss.swim.matching.TestConstants.GROUP_INBOX_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_INBOX_1;
import static org.junit.jupiter.api.Assertions.*;

import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import org.junit.jupiter.api.Test;

class InboxMapperTest {
    private final InboxMapper inboxMapper = new InboxMapperImpl();

    @Test
    void testMapUserInbox() {
        final UserDmsInbox userDmsInbox = inboxMapper.toUserInbox(USER_INBOX_1, USER_1);
        // test
        assertEquals(USER_INBOX_1.getCoo(), userDmsInbox.getCoo());
        assertEquals(USER_INBOX_1.getName(), userDmsInbox.getName());
        assertEquals(USER_INBOX_1.getOwnerLhmObjectId(), userDmsInbox.getOwnerLhmObjectId());
        assertEquals(USER_INBOX_1.getOu(), userDmsInbox.getOu());
        assertEquals(USER_INBOX_1.getDmsTenant(), userDmsInbox.getDmsTenant());

        assertEquals(USER_1.lhmObjectId(), userDmsInbox.getUser().lhmObjectId());
        assertEquals(USER_1.username(), userDmsInbox.getUser().username());
        assertEquals(USER_1.firstname(), userDmsInbox.getUser().firstname());
        assertEquals(USER_1.surname(), userDmsInbox.getUser().surname());
        assertEquals(USER_1.ou(), userDmsInbox.getUser().ou());
        assertEquals(USER_1.officeAddress().getStreet(), userDmsInbox.getUser().officeAddress().getStreet());
        assertEquals(USER_1.officeAddress().getPostalcode(), userDmsInbox.getUser().officeAddress().getPostalcode());
        assertEquals(USER_1.officeAddress().getCity(), userDmsInbox.getUser().officeAddress().getCity());
        assertEquals(USER_1.postalAddress().getStreet(), userDmsInbox.getUser().postalAddress().getStreet());
        assertEquals(USER_1.postalAddress().getPostalcode(), userDmsInbox.getUser().postalAddress().getPostalcode());
        assertEquals(USER_1.postalAddress().getCity(), userDmsInbox.getUser().postalAddress().getCity());
    }

    @Test
    void testMapGroupInbox() {
        final GroupDmsInbox groupDmsInbox = inboxMapper.toGroupInbox(GROUP_INBOX_1, USER_1);
        // test
        assertEquals(GROUP_INBOX_1.getCoo(), groupDmsInbox.getCoo());
        assertEquals(USER_1.username(), groupDmsInbox.getUsername());
        assertEquals(GROUP_INBOX_1.getName(), groupDmsInbox.getName());
        assertEquals(GROUP_INBOX_1.getOu(), groupDmsInbox.getOu());
        assertEquals(GROUP_INBOX_1.getDmsTenant(), groupDmsInbox.getDmsTenant());
    }
}

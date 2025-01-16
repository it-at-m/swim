package de.muenchen.oss.swim.matching.domain.mapper;

import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.User;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InboxMapper {
    @Mapping(source = "dmsInbox.ou", target = "ou")
    UserDmsInbox toUserInbox(DmsInbox dmsInbox, User user);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "dmsInbox.ou", target = "ou")
    GroupDmsInbox toGroupInbox(DmsInbox dmsInbox, User user);

    List<GroupDmsInbox> toGroupInboxes(List<DmsInbox> dmsInboxes);
}

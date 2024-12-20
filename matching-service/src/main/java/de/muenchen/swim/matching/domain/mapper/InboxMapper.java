package de.muenchen.swim.matching.domain.mapper;

import de.muenchen.swim.matching.domain.model.DmsInbox;
import de.muenchen.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.swim.matching.domain.model.User;
import de.muenchen.swim.matching.domain.model.UserDmsInbox;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InboxMapper {
    @Mapping(source = "dmsInbox.ou", target = "ou")
    UserDmsInbox toUserInbox(DmsInbox dmsInbox, User user);

    GroupDmsInbox toGroupInbox(DmsInbox dmsInbox);

    List<GroupDmsInbox> toGroupInboxes(List<DmsInbox> dmsInboxes);
}

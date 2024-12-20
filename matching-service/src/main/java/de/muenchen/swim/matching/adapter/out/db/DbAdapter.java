package de.muenchen.swim.matching.adapter.out.db;

import de.muenchen.swim.matching.application.port.out.StoreMatchingEntriesOutPort;
import de.muenchen.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.swim.matching.domain.model.UserDmsInbox;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class DbAdapter implements StoreMatchingEntriesOutPort {
    private final DbInboxMapper dbInboxMapper;
    private final UserInboxRepository userInboxRepository;
    private final GroupInboxRepository groupInboxRepository;

    @Override
    @Transactional
    public void storeUserInboxes(final List<UserDmsInbox> userInboxes) {
        log.info("Storing {} user inboxes", userInboxes.size());
        // group by dms tenant
        final Map<String, List<UserDmsInbox>> groupedUserInboxes = userInboxes.stream()
                .collect(Collectors.groupingBy(UserDmsInbox::getDmsTenant));
        // replace dms tenant one by one
        for (final Map.Entry<String, List<UserDmsInbox>> userInboxesByTenant : groupedUserInboxes.entrySet()) {
            userInboxRepository.deleteByDmsTenant(userInboxesByTenant.getKey());
            userInboxRepository.saveAll(DbInboxMapper.toUserInboxes(userInboxesByTenant.getValue()));
        }
    }

    @Override
    @Transactional
    public void storeGroupInboxes(final List<GroupDmsInbox> groupInboxes) {
        log.info("Storing {} group inboxes", groupInboxes.size());
        // group by dms tenant
        final Map<String, List<GroupDmsInbox>> groupedGroupInboxes = groupInboxes.stream()
                .collect(Collectors.groupingBy(GroupDmsInbox::getDmsTenant));
        // replace dms tenant one by one
        for (final Map.Entry<String, List<GroupDmsInbox>> groupInboxesByTenant : groupedGroupInboxes.entrySet()) {
            groupInboxRepository.deleteByDmsTenant(groupInboxesByTenant.getKey());
            groupInboxRepository.saveAll(dbInboxMapper.toDbGroupInboxes(groupInboxesByTenant.getValue()));
        }
    }
}

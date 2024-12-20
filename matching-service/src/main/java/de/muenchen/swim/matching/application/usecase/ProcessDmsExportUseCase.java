package de.muenchen.swim.matching.application.usecase;

import de.muenchen.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.swim.matching.application.port.out.StoreMatchingEntriesOutPort;
import de.muenchen.swim.matching.application.port.out.UserInformationOutPort;
import de.muenchen.swim.matching.domain.mapper.InboxMapper;
import de.muenchen.swim.matching.domain.model.DmsInbox;
import de.muenchen.swim.matching.domain.model.ImportReport;
import de.muenchen.swim.matching.domain.model.User;
import de.muenchen.swim.matching.domain.model.UserDmsInbox;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDmsExportUseCase implements ProcessDmsExportInPort {
    private final InboxMapper inboxMapper;
    private final UserInformationOutPort userInformationOutPort;
    private final StoreMatchingEntriesOutPort storeMatchingEntriesOutPort;

    /**
     * Process inboxes as user and group inboxes.
     * See {@link ProcessDmsExportUseCase#processUserInboxes} and
     * {@link ProcessDmsExportUseCase#processGroupInboxes}.
     *
     * @param dmsInboxes Inboxes to be processed.
     * @return Report of the import.
     */
    @Override
    public ImportReport process(final List<DmsInbox> dmsInboxes) {
        log.info("Starting processing {} inboxes", dmsInboxes.size());
        final ImportReport importReport = new ImportReport();
        importReport.setInputInboxes(dmsInboxes.size());
        importReport.setDmsTenants(dmsInboxes.stream().map(DmsInbox::getDmsTenant).collect(Collectors.toSet()));
        // group inboxes by type
        final Map<DmsInbox.InboxType, List<DmsInbox>> groupedInboxes = dmsInboxes.stream().collect(Collectors.groupingBy(DmsInbox::getType));
        // process different types
        if (groupedInboxes.containsKey(DmsInbox.InboxType.GROUP)) {
            final List<DmsInbox> groupDmsInboxes = groupedInboxes.get(DmsInbox.InboxType.GROUP);
            importReport.setGroupInboxes(groupDmsInboxes.size());
            this.processGroupInboxes(groupDmsInboxes, importReport);
        }
        if (groupedInboxes.containsKey(DmsInbox.InboxType.USER)) {
            final List<DmsInbox> userDmsInboxes = groupedInboxes.get(DmsInbox.InboxType.USER);
            importReport.setUserInboxes(userDmsInboxes.size());
            this.processUserInboxes(userDmsInboxes, importReport);
        }
        return importReport;
    }

    /**
     * Process user inboxes. Enriching them with ldap data and writing them into matching db.
     *
     * @param dmsInboxes The inboxes to process.
     * @param importReport The report to write processing information into.
     */
    protected void processUserInboxes(final List<DmsInbox> dmsInboxes, final ImportReport importReport) {
        log.debug("Starting processing of {} user inboxes", dmsInboxes.size());
        // get user information
        final Map<String, User> users = userInformationOutPort.getAllUsers()
                .stream().collect(Collectors.toMap(User::lhmObjectId, i -> i));
        // combine data
        final Map<Boolean, List<UserDmsInbox>> userDmsInboxes = dmsInboxes.stream()
                .map(i -> inboxMapper.toUserInbox(i, users.get(i.getOwnerLhmObjectId())))
                .collect(Collectors.groupingBy(i -> i.getUser() != null));
        // inboxes not found in ldap
        if (userDmsInboxes.containsKey(false)) {
            final List<UserDmsInbox> incompleteUserInboxes = userDmsInboxes.get(false);
            log.warn("Couldn't find {} users in ldap", incompleteUserInboxes.size());
            importReport.setUnresolvableUserInboxes(incompleteUserInboxes.size());
        }
        // store enriched inboxes
        if (userDmsInboxes.containsKey(true)) {
            this.storeMatchingEntriesOutPort.storeUserInboxes(userDmsInboxes.get(true));
            importReport.setImportedUserInboxes(userDmsInboxes.get(true).size());
        }
    }

    /**
     * Process group inboxes. Writing them into matching db.
     *
     * @param dmsInboxes The inboxes to process.
     * @param importReport The report to write processing information into.
     */
    protected void processGroupInboxes(final List<DmsInbox> dmsInboxes, final ImportReport importReport) {
        log.debug("Starting processing of {} group inboxes", dmsInboxes.size());
        this.storeMatchingEntriesOutPort.storeGroupInboxes(inboxMapper.toGroupInboxes(dmsInboxes));
        importReport.setImportedGroupInboxes(dmsInboxes.size());
    }
}

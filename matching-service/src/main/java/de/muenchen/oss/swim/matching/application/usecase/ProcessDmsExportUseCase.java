package de.muenchen.oss.swim.matching.application.usecase;

import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.oss.swim.matching.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.matching.application.port.out.ExportParsingOutPort;
import de.muenchen.oss.swim.matching.application.port.out.StoreMatchingEntriesOutPort;
import de.muenchen.oss.swim.matching.application.port.out.UserInformationOutPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.mapper.InboxMapper;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.ImportReport;
import de.muenchen.oss.swim.matching.domain.model.User;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import java.io.IOException;
import java.io.InputStream;
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
    private final ExportParsingOutPort exportParsingOutPort;
    private final DmsOutPort dmsOutPort;
    private final SwimMatchingProperties swimMatchingProperties;

    @Override
    public ImportReport processExport(final InputStream csvExport) throws CsvParsingException {
        final List<DmsInbox> inboxes = exportParsingOutPort.parseCsv(csvExport);
        return this.process(inboxes);
    }

    @Override
    public ImportReport triggerProcessingViaDms() throws CsvParsingException, IOException {
        try (InputStream csvExport = this.dmsOutPort.getExportContent()) {
            final List<DmsInbox> inboxes = exportParsingOutPort.parseCsv(csvExport);
            return this.process(inboxes);
        } catch (final CsvParsingException | RuntimeException | IOException e) {
            log.error("Error while processing export via DMS", e);
            throw e;
        }
    }

    /**
     * Process inboxes as user and group inboxes.
     * See {@link ProcessDmsExportUseCase#processUserInboxes} and
     * {@link ProcessDmsExportUseCase#processGroupInboxes}.
     *
     * @param dmsInboxes Inboxes to be processed.
     * @return Report of the import.
     */
    protected ImportReport process(final List<DmsInbox> dmsInboxes) {
        final ImportReport importReport = new ImportReport();
        importReport.setInputInboxes(dmsInboxes.size());
        // filter inbox tenant
        final List<DmsInbox> dmsInboxesFiltered = dmsInboxes.stream()
                .filter(i -> swimMatchingProperties.getDmsTenants().contains(i.getDmsTenant()))
                .toList();
        importReport.setFilteredInboxes(dmsInboxesFiltered.size());
        log.info("Starting processing of {} whitelisted inboxes ({} before filtering)", dmsInboxesFiltered.size(), dmsInboxes.size());
        importReport.setDmsTenants(dmsInboxesFiltered.stream().map(DmsInbox::getDmsTenant).collect(Collectors.toSet()));
        // group inboxes by type
        final Map<DmsInbox.InboxType, List<DmsInbox>> groupedInboxes = dmsInboxesFiltered.stream().collect(Collectors.groupingBy(DmsInbox::getType));
        // process group inboxes
        if (groupedInboxes.containsKey(DmsInbox.InboxType.GROUP)) {
            final List<DmsInbox> groupDmsInboxes = groupedInboxes.get(DmsInbox.InboxType.GROUP);
            importReport.getGroupInboxes().setInput(groupDmsInboxes.size());
            this.processGroupInboxes(groupDmsInboxes, importReport);
        }
        // process user inboxes
        if (groupedInboxes.containsKey(DmsInbox.InboxType.USER)) {
            final List<DmsInbox> userDmsInboxes = groupedInboxes.get(DmsInbox.InboxType.USER);
            importReport.getUserInboxes().setInput(userDmsInboxes.size());
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
            log.warn("Couldn't find {} users in ldap for user inboxes", incompleteUserInboxes.size());
            importReport.getUserInboxes().setUnresolvable(incompleteUserInboxes.size());
        }
        // store enriched inboxes
        if (userDmsInboxes.containsKey(true)) {
            this.storeMatchingEntriesOutPort.storeUserInboxes(userDmsInboxes.get(true));
            importReport.getUserInboxes().setImported(userDmsInboxes.get(true).size());
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
        // get user information
        final Map<String, User> users = userInformationOutPort.getAllUsers()
                .stream().collect(Collectors.toMap(User::lhmObjectId, i -> i));
        // combine data
        final Map<Boolean, List<GroupDmsInbox>> groupDmsInboxes = dmsInboxes.stream()
                .map(i -> inboxMapper.toGroupInbox(i, users.get(i.getOwnerLhmObjectId())))
                .collect(Collectors.groupingBy(i -> i.getUsername() != null));
        // inboxes not found in ldap
        if (groupDmsInboxes.containsKey(false)) {
            final List<GroupDmsInbox> incompleteGroupInboxes = groupDmsInboxes.get(false);
            log.warn("Couldn't find {} users in ldap for group inboxes", incompleteGroupInboxes.size());
            importReport.getGroupInboxes().setUnresolvable(incompleteGroupInboxes.size());
        }
        // store enriched inboxes
        if (groupDmsInboxes.containsKey(true)) {
            this.storeMatchingEntriesOutPort.storeGroupInboxes(groupDmsInboxes.get(true));
            importReport.getGroupInboxes().setImported(groupDmsInboxes.get(true).size());
        }
    }
}

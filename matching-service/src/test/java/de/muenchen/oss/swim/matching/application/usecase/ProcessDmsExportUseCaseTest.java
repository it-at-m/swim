package de.muenchen.oss.swim.matching.application.usecase;

import static de.muenchen.oss.swim.matching.TestConstants.GROUP_INBOX_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_2;
import static de.muenchen.oss.swim.matching.TestConstants.USER_INBOX_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_INBOX_2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.matching.application.port.out.StoreMatchingEntriesOutPort;
import de.muenchen.oss.swim.matching.application.port.out.UserInformationOutPort;
import de.muenchen.oss.swim.matching.domain.mapper.InboxMapper;
import de.muenchen.oss.swim.matching.domain.mapper.InboxMapperImpl;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import de.muenchen.oss.swim.matching.domain.model.GroupDmsInbox;
import de.muenchen.oss.swim.matching.domain.model.UserDmsInbox;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessDmsExportUseCaseTest {
    private final InboxMapper inboxMapper = new InboxMapperImpl();
    private final UserInformationOutPort userInformationOutPort = mock();
    private final StoreMatchingEntriesOutPort storeMatchingEntriesOutPort = mock();
    @Spy
    private final ProcessDmsExportUseCase processDmsExportUseCase = new ProcessDmsExportUseCase(
            inboxMapper, userInformationOutPort, storeMatchingEntriesOutPort);

    @Test
    void testProcess() {
        // setup
        when(userInformationOutPort.getAllUsers()).thenReturn(List.of(USER_1, USER_2));

        // call
        final List<DmsInbox> dmsInboxes = List.of(USER_INBOX_1, USER_INBOX_2, GROUP_INBOX_1);
        processDmsExportUseCase.process(dmsInboxes);

        // test
        // user
        verify(processDmsExportUseCase, times(1)).processUserInboxes(eq(List.of(USER_INBOX_1, USER_INBOX_2)), any());
        final UserDmsInbox userDmsInbox1 = inboxMapper.toUserInbox(USER_INBOX_1, USER_1);
        final UserDmsInbox userDmsInbox2 = inboxMapper.toUserInbox(USER_INBOX_2, USER_2);
        verify(storeMatchingEntriesOutPort, times(1)).storeUserInboxes(eq(List.of(userDmsInbox1, userDmsInbox2)));
        // group
        verify(processDmsExportUseCase, times(1)).processGroupInboxes(eq(List.of(GROUP_INBOX_1)), any());
        final GroupDmsInbox groupDmsInbox1 = inboxMapper.toGroupInbox(GROUP_INBOX_1);
        verify(storeMatchingEntriesOutPort, times(1)).storeGroupInboxes(eq(List.of(groupDmsInbox1)));
    }
}

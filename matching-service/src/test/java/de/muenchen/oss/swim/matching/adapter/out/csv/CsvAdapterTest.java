package de.muenchen.oss.swim.matching.adapter.out.csv;

import static de.muenchen.oss.swim.matching.TestConstants.GROUP_INBOX_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_INBOX_1;
import static de.muenchen.oss.swim.matching.TestConstants.USER_INBOX_2;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.matching.TestConstants;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { CsvAdapter.class, DmsExportMapperImpl.class })
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@ExtendWith(MockitoExtension.class)
class CsvAdapterTest {
    @Autowired
    @MockitoSpyBean
    private CsvAdapter csvAdapter;

    @Test
    void testParseCsv() throws CsvParsingException {
        final List<DmsInbox> inboxes = this.csvAdapter.parseCsv(getClass().getResourceAsStream("/files/test.csv"));
        assertEquals(List.of(USER_INBOX_1, USER_INBOX_2, GROUP_INBOX_1), inboxes);
    }
}

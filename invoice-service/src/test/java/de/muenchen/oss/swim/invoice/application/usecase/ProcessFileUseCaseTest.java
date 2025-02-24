package de.muenchen.oss.swim.invoice.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.muenchen.oss.swim.invoice.TestConstants;
import de.muenchen.oss.swim.invoice.application.port.out.InvoiceServiceOutPort;
import de.muenchen.oss.swim.invoice.configuration.InvoiceMeter;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { ProcessFileUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ProcessFileUseCaseTest {
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @MockitoBean
    private InvoiceServiceOutPort invoiceServiceOutPort;
    @MockitoBean
    private FileEventOutPort fileEventOutPort;
    @MockitoBean
    private InvoiceMeter invoiceMeter;
    @MockitoSpyBean
    @Autowired
    private ProcessFileUseCase processFileUseCase;

    private final static String BUCKET = "test-bucket";
    private final static String FILE_NAME_WITHOUT_EXTENSION = "test-COO.123.123.123-asd";
    private final static String FILE_NAME = String.format("%s.pdf", FILE_NAME_WITHOUT_EXTENSION);
    private final static String FILE_PATH = String.format("test-path/%s", FILE_NAME);
    private final static File FILE = new File(BUCKET, FILE_PATH);
    private final static String FILE_PRESIGNED_URL = String.format("http://localhost:9001/%s/%s", BUCKET, FILE_PATH);
    private final static String USE_CASE = "test-usecase";
    private final static FileEvent FILE_EVENT = new FileEvent(USE_CASE, FILE_PRESIGNED_URL, null);

    @Test
    void testProcessFile() throws UnknownUseCaseException, PresignedUrlException {
        // call
        processFileUseCase.processFile(FILE_EVENT, FILE);
        // test
        verify(invoiceServiceOutPort, times(1)).createInvoice(eq(FILE_NAME), any());
        verify(fileEventOutPort, times(1)).fileFinished(eq(FILE_EVENT));
        verify(invoiceMeter, times(1)).incrementProcessed(eq(USE_CASE));
    }
}

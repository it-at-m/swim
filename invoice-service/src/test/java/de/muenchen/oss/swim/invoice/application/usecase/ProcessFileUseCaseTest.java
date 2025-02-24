package de.muenchen.oss.swim.invoice.application.usecase;

import de.muenchen.oss.swim.invoice.TestConstants;
import de.muenchen.oss.swim.invoice.application.port.out.InvoiceServiceOutPort;
import de.muenchen.oss.swim.invoice.configuration.InvoiceMeter;
import de.muenchen.oss.swim.invoice.configuration.SwimInvoiceProperties;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { SwimInvoiceProperties.class, ProcessFileUseCase.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ProcessFileUseCaseTest {
    @MockitoSpyBean
    @Autowired
    private SwimInvoiceProperties swimInvoiceProperties;
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


}

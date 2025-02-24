package de.muenchen.oss.swim.invoice.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.muenchen.oss.swim.invoice.TestConstants;
import de.muenchen.oss.swim.invoice.domain.model.UseCase;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimInvoiceProperties.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class SwimInvoicePropertiesTest {
    @Autowired
    private SwimInvoiceProperties swimInvoiceProperties;

    @Test
    void testFindUseCase() throws UnknownUseCaseException {
        final String useCaseName = "metadata-inbox";
        // call
        final UseCase useCase = swimInvoiceProperties.findUseCase(useCaseName);
        // test
        assertEquals(useCaseName, useCase.getName());
    }

    @Test
    void testFindUseCase_UnknownCase() {
        final String unknownUseCase = "non-existent-case";
        assertThrows(UnknownUseCaseException.class, () -> swimInvoiceProperties.findUseCase(unknownUseCase));
    }
}

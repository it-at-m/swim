package de.muenchen.oss.swim.dms.configuration;

import static org.junit.jupiter.api.Assertions.*;

import de.muenchen.oss.swim.dms.TestConstants;
import de.muenchen.oss.swim.dms.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDmsProperties.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class SwimDmsPropertiesTest {
    @Autowired
    private SwimDmsProperties swimDmsProperties;

    @Test
    void testFindUseCase() throws UnknownUseCaseException {
        final String useCaseName = "metadata-inbox";
        // call
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        // test
        assertEquals(useCaseName, useCase.getName());
    }
}

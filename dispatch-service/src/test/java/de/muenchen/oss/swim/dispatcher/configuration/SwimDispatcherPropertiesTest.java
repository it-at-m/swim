package de.muenchen.oss.swim.dispatcher.configuration;

import static de.muenchen.oss.swim.dispatcher.TestConstants.TEST_USE_CASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDispatcherProperties.class })
@EnableConfigurationProperties
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class SwimDispatcherPropertiesTest {
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    @Test
    void testFindUseCase() throws UseCaseException {
        // found
        final UseCase useCase = this.swimDispatcherProperties.findUseCase(TEST_USE_CASE);
        assertEquals(TEST_USE_CASE, useCase.getName());
        // not found
        assertThrows(UseCaseException.class, () -> swimDispatcherProperties.findUseCase("unknown-usecase"));
    }
}

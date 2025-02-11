package de.muenchen.oss.swim.dispatcher.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { SwimDispatcherProperties.class })
@EnableConfigurationProperties
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class UseCaseTest {
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;

    private final UseCase useCase;

    public UseCaseTest() {
        this.useCase = new UseCase();
        this.useCase.setPath("testPath/");
    }

    @Test
    void testGetPathWithoutSlash() {
        assertEquals("testPath", useCase.getPathWithoutSlash());
    }

    @Test
    void testGetDispatchPath() {
        assertEquals("testPath/inProcess", useCase.getDispatchPath(swimDispatcherProperties));
    }

    @Test
    void testGetFinishedPath() {
        assertEquals("testPath/_finished", useCase.getFinishedPath(swimDispatcherProperties));
    }

    @Test
    void testGetFinishedPath_File() {
        assertEquals("testPath/_finished/test/asd.pdf", useCase.getFinishedPath(swimDispatcherProperties, "testPath/inProcess/test/asd.pdf"));
    }

    @Test
    void testGetRawPath() {
        assertEquals("test/asd.pdf", useCase.getRawPath(swimDispatcherProperties, "testPath/inProcess/test/asd.pdf"));
        assertEquals("test/asd.pdf", useCase.getRawPath(swimDispatcherProperties, "testPath/_finished/test/asd.pdf"));
        assertEquals("test", useCase.getRawPath(swimDispatcherProperties, "testPath/inProcess/test"));
        assertEquals("test", useCase.getRawPath(swimDispatcherProperties, "testPath/_finished/test"));
    }
}

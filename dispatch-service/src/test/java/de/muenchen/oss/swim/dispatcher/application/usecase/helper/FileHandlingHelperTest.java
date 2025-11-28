package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static de.muenchen.oss.swim.dispatcher.TestConstants.BUCKET;
import static de.muenchen.oss.swim.dispatcher.TestConstants.FILE1;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = { SwimDispatcherProperties.class, FileHandlingHelper.class })
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
public class FileHandlingHelperTest {

    @MockitoBean
    private DispatchMeter dispatchMeter;
    @MockitoBean
    private FileSystemOutPort fileSystemOutPort;
    @Autowired
    private SwimDispatcherProperties swimDispatcherProperties;
    @MockitoSpyBean
    @Autowired
    private FileHandlingHelper fileHandlingHelper;

    @Test
    void testMarkFileError() {
        // setup
        final Exception testException = new RuntimeException("Test");
        // call
        fileHandlingHelper.markFileError(FILE1, "SWIM_State", testException);
        // test
        verify(fileSystemOutPort).tagFile(eq(BUCKET), eq(FILE1.path()), eq(Map.of(
                "SWIM_State", "error",
                "errorClass", "java.lang.RuntimeException",
                "errorMessage", "Test")));
    }

    @Test
    void testFinishFile() {
        // setup
        final UseCase useCase = swimDispatcherProperties.getUseCases().getFirst();
        // call
        fileHandlingHelper.finishFile(useCase, BUCKET, FILE1.path());
        // test
        verify(fileSystemOutPort).moveFile(eq(BUCKET), eq(FILE1.path()), eq("test/finished/path/test.pdf"));
        verify(fileSystemOutPort).tagFile(eq(BUCKET), eq("test/finished/path/test.pdf"), eq(Map.of(
                "SWIM_State", "finished")));
    }
}

package de.muenchen.oss.swim.dms.application.usecase;

import static de.muenchen.oss.swim.dms.application.usecase.helper.DmsHelper.SHADOW_PROCEDURE_NAME_PATTERN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CleanupShadowFilesUseCaseTest {
    private static final String CRON = "0 0 * * * *";
    private static final String USERNAME = "shadow-user";
    private static final String FILE_COO_1 = "COO.file.1";
    private static final String FILE_COO_2 = "COO.file.2";
    private static final String PROCEDURE_COO_1 = "COO.procedure.1";
    private static final String PROCEDURE_COO_2 = "COO.procedure.2";
    private static final String EXPECTED_PROCEDURE_NAME = LocalDate.now().minusMonths(1).format(SHADOW_PROCEDURE_NAME_PATTERN);

    @Mock
    private SwimDmsProperties swimDmsProperties;

    @Mock
    private DmsOutPort dmsOutPort;

    @InjectMocks
    private CleanupShadowFilesUseCase cleanupShadowFilesUseCase;

    @BeforeEach
    void setUp() {
        when(swimDmsProperties.getShadowFileCleanup()).thenReturn(buildCleanupProperties(List.of(FILE_COO_1, FILE_COO_2)));
    }

    @Test
    void cleanupShadowFiles_archivesProcedureWhenPresent() {
        final DmsTarget fileDmsTarget = fileTarget(FILE_COO_1);
        when(dmsOutPort.getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_1));

        cleanupShadowFilesUseCase.cleanupShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort).archiveObject(eq(procedureTarget(PROCEDURE_COO_1, fileDmsTarget)));
    }

    @Test
    void cleanupShadowFiles_skipsArchiveWhenProcedureMissing() {
        when(swimDmsProperties.getShadowFileCleanup()).thenReturn(buildCleanupProperties(List.of(FILE_COO_1)));
        final DmsTarget fileDmsTarget = fileTarget(FILE_COO_1);
        when(dmsOutPort.getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.empty());

        cleanupShadowFilesUseCase.cleanupShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort, never()).archiveObject(eq(procedureTarget(PROCEDURE_COO_1, fileDmsTarget)));
    }

    @Test
    void cleanupShadowFiles_handlesMultipleFileCoos() {
        final DmsTarget firstFileDmsTarget = fileTarget(FILE_COO_1);
        final DmsTarget secondFileDmsTarget = fileTarget(FILE_COO_2);
        when(dmsOutPort.getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_1));
        when(dmsOutPort.getProcedureCooByName(eq(secondFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_2));

        cleanupShadowFilesUseCase.cleanupShadowFiles();

        verifyArchivedProcedure(firstFileDmsTarget, PROCEDURE_COO_1);
        verifyArchivedProcedure(secondFileDmsTarget, PROCEDURE_COO_2);
    }

    @Test
    void cleanupShadowFiles_logsAndContinuesOnRuntimeException() {
        final DmsTarget firstFileDmsTarget = fileTarget(FILE_COO_1);
        final DmsTarget secondFileDmsTarget = fileTarget(FILE_COO_2);
        when(dmsOutPort.getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenThrow(new RuntimeException("DMS failed"));
        when(dmsOutPort.getProcedureCooByName(eq(secondFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_2));

        cleanupShadowFilesUseCase.cleanupShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verifyArchivedProcedure(secondFileDmsTarget, PROCEDURE_COO_2);
    }

    private SwimDmsProperties.ShadowFileCleanup buildCleanupProperties(final List<String> coos) {
        final SwimDmsProperties.ShadowFileCleanup cleanupProperties = new SwimDmsProperties.ShadowFileCleanup();
        cleanupProperties.setCron(CRON);
        cleanupProperties.setUsername(USERNAME);
        cleanupProperties.setCoos(coos);
        return cleanupProperties;
    }

    private DmsTarget fileTarget(final String fileCoo) {
        return new DmsTarget(fileCoo, USERNAME, null, null);
    }

    private DmsTarget procedureTarget(final String procedureCoo, final DmsTarget fileDmsTarget) {
        return new DmsTarget(procedureCoo, fileDmsTarget);
    }

    private void verifyArchivedProcedure(final DmsTarget fileDmsTarget, final String procedureCoo) {
        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort).archiveObject(eq(procedureTarget(procedureCoo, fileDmsTarget)));
    }
}

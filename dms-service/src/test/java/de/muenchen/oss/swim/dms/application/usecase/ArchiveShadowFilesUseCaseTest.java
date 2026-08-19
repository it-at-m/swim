package de.muenchen.oss.swim.dms.application.usecase;

import static de.muenchen.oss.swim.dms.application.usecase.helper.DmsHelper.SHADOW_PROCEDURE_NAME_PATTERN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsRequestContext;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchiveShadowFilesUseCaseTest {
    private static final String CRON = "0 0 * * * *";
    private static final String USERNAME = "shadow-user";
    private static final String SUBJECT_AREA_COO_1 = "COO.subject-area.1";
    private static final String SUBJECT_AREA_COO_2 = "COO.subject-area.2";
    private static final String FILE_COO_1 = "COO.file.1";
    private static final String FILE_COO_2 = "COO.file.2";
    private static final String PROCEDURE_COO_1 = "COO.procedure.1";
    private static final String PROCEDURE_COO_2 = "COO.procedure.2";
    private static final String EXPECTED_PROCEDURE_NAME = LocalDate.now().minusMonths(1).format(SHADOW_PROCEDURE_NAME_PATTERN);
    private static final DmsRequestContext REQUEST_CONTEXT = new DmsRequestContext(USERNAME, null, null);

    @Mock
    private SwimDmsProperties swimDmsProperties;

    @Mock
    private DmsOutPort dmsOutPort;

    @InjectMocks
    private ArchiveShadowFilesUseCase archiveShadowFilesUseCase;

    @BeforeEach
    void setUp() {
        when(swimDmsProperties.getShadowFileArchiving()).thenReturn(buildArchiveProperties(List.of(SUBJECT_AREA_COO_1, SUBJECT_AREA_COO_2)));
    }

    @Test
    void archiveShadowFiles_archivesProcedureWhenPresent() {
        when(swimDmsProperties.getShadowFileArchiving()).thenReturn(buildArchiveProperties(List.of(SUBJECT_AREA_COO_1)));
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_1, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_1));
        final DmsTarget fileDmsTarget = new DmsTarget(FILE_COO_1, REQUEST_CONTEXT);
        when(dmsOutPort.getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_1));

        archiveShadowFilesUseCase.archiveShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort).archiveObject(eq(new DmsTarget(PROCEDURE_COO_1, fileDmsTarget)));
    }

    @Test
    void archiveShadowFiles_archivesProcedureFromPreviousDecemberWhenRunInJanuary() {
        final LocalDate runDate = LocalDate.of(2027, 1, 15);
        final String expectedProcedureName = runDate.minusMonths(1).format(SHADOW_PROCEDURE_NAME_PATTERN);
        when(swimDmsProperties.getShadowFileArchiving()).thenReturn(buildArchiveProperties(List.of(SUBJECT_AREA_COO_1)));
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_1, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_1));
        final DmsTarget fileDmsTarget = new DmsTarget(FILE_COO_1, REQUEST_CONTEXT);
        when(dmsOutPort.getProcedureCooByName(eq(fileDmsTarget), eq(expectedProcedureName))).thenReturn(Optional.of(PROCEDURE_COO_1));

        try (MockedStatic<LocalDate> localDateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            localDateMock.when(LocalDate::now).thenReturn(runDate);

            archiveShadowFilesUseCase.archiveShadowFiles();
        }

        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq("2026-12"));
        verify(dmsOutPort).archiveObject(eq(new DmsTarget(PROCEDURE_COO_1, fileDmsTarget)));
    }

    @Test
    void archiveShadowFiles_skipsArchiveWhenProcedureMissing() {
        when(swimDmsProperties.getShadowFileArchiving()).thenReturn(buildArchiveProperties(List.of(SUBJECT_AREA_COO_1)));
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_1, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_1));
        final DmsTarget fileDmsTarget = new DmsTarget(FILE_COO_1, REQUEST_CONTEXT);
        when(dmsOutPort.getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.empty());

        archiveShadowFilesUseCase.archiveShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort, never()).archiveObject(eq(new DmsTarget(PROCEDURE_COO_1, fileDmsTarget)));
    }

    @Test
    void archiveShadowFiles_handlesMultipleFileCoos() {
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_1, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_1));
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_2, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_2));
        final DmsTarget firstFileDmsTarget = new DmsTarget(FILE_COO_1, REQUEST_CONTEXT);
        final DmsTarget secondFileDmsTarget = new DmsTarget(FILE_COO_2, REQUEST_CONTEXT);
        when(dmsOutPort.getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_1));
        when(dmsOutPort.getProcedureCooByName(eq(secondFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_2));

        archiveShadowFilesUseCase.archiveShadowFiles();

        verifyArchivedProcedure(firstFileDmsTarget, PROCEDURE_COO_1);
        verifyArchivedProcedure(secondFileDmsTarget, PROCEDURE_COO_2);
    }

    @Test
    void archiveShadowFiles_logsAndContinuesOnRuntimeException() {
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_1, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_1));
        when(dmsOutPort.getSubjectAreaFiles(eq(new DmsTarget(SUBJECT_AREA_COO_2, REQUEST_CONTEXT)))).thenReturn(List.of(FILE_COO_2));
        final DmsTarget firstFileDmsTarget = new DmsTarget(FILE_COO_1, REQUEST_CONTEXT);
        final DmsTarget secondFileDmsTarget = new DmsTarget(FILE_COO_2, REQUEST_CONTEXT);
        when(dmsOutPort.getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenThrow(new RuntimeException("DMS failed"));
        when(dmsOutPort.getProcedureCooByName(eq(secondFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME))).thenReturn(Optional.of(PROCEDURE_COO_2));

        archiveShadowFilesUseCase.archiveShadowFiles();

        verify(dmsOutPort).getProcedureCooByName(eq(firstFileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verifyArchivedProcedure(secondFileDmsTarget, PROCEDURE_COO_2);
    }

    private SwimDmsProperties.ShadowFileArchiving buildArchiveProperties(final List<String> coos) {
        final SwimDmsProperties.ShadowFileArchiving archiveProperties = new SwimDmsProperties.ShadowFileArchiving();
        archiveProperties.setCron(CRON);
        archiveProperties.setUsername(USERNAME);
        archiveProperties.setCoos(coos);
        return archiveProperties;
    }

    private void verifyArchivedProcedure(final DmsTarget fileDmsTarget, final String procedureCoo) {
        verify(dmsOutPort).getProcedureCooByName(eq(fileDmsTarget), eq(EXPECTED_PROCEDURE_NAME));
        verify(dmsOutPort).archiveObject(eq(new DmsTarget(procedureCoo, REQUEST_CONTEXT)));
    }
}

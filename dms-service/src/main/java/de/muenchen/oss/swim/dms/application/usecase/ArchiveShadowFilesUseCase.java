package de.muenchen.oss.swim.dms.application.usecase;

import static de.muenchen.oss.swim.dms.application.usecase.helper.DmsHelper.SHADOW_PROCEDURE_NAME_PATTERN;

import de.muenchen.oss.swim.dms.application.port.in.ArchiveShadowFilesInPort;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveShadowFilesUseCase implements ArchiveShadowFilesInPort {
    private final SwimDmsProperties swimDmsProperties;
    private final DmsOutPort dmsOutPort;

    @Override
    public void archiveShadowFiles() {
        log.info("Starting archiving shadow files");
        final SwimDmsProperties.ShadowFileArchiving archiveProperties = swimDmsProperties.getShadowFileArchiving();
        // construct procedure name of last month
        final LocalDate previousMonthDate = LocalDate.now().minusMonths(1);
        final String procedureName = previousMonthDate.format(SHADOW_PROCEDURE_NAME_PATTERN);
        // for each SubjectArea COO
        for (final String subjectAreaCoo : archiveProperties.getCoos()) {
            log.debug("Processing SubjectArea {}", subjectAreaCoo);
            final DmsTarget dmsTarget = new DmsTarget(subjectAreaCoo, archiveProperties.getUsername(), archiveProperties.getJobOe(),
                    archiveProperties.getJobPosition());
            try {
                final List<String> fileCoos = dmsOutPort.getSubjectAreaFiles(dmsTarget);
                // for each File in the SubjectArea
                for (final String fileCoo : fileCoos) {
                    log.info("Processing Procedure {} in File {}", procedureName, fileCoo);
                    archiveProcedure(fileCoo, procedureName, dmsTarget);
                }
            } catch (final RuntimeException e) {
                log.error("Error while loading Files for SubjectArea {}", subjectAreaCoo, e);
            }
        }
        log.info("Finished archiving shadow files");
    }

    private void archiveProcedure(final String fileCoo, final String procedureName, final DmsTarget dmsTarget) {
        final DmsTarget fileDmsTarget = new DmsTarget(fileCoo, dmsTarget);
        try {
            // search for Procedure
            final Optional<String> procedureCoo = dmsOutPort.getProcedureCooByName(fileDmsTarget, procedureName);
            // archive Procedure if present
            if (procedureCoo.isPresent()) {
                log.info("Archiving Procedure {}", procedureCoo.get());
                final DmsTarget procedureDmsTarget = new DmsTarget(procedureCoo.get(), fileDmsTarget);
                dmsOutPort.archiveObject(procedureDmsTarget);
            } else {
                log.warn("No shadow file Procedure {} exists in File {}", procedureName, fileCoo);
            }
        } catch (final RuntimeException e) {
            if (e instanceof DmsException de &&
                    de.getDmsError() != null && de.getDmsError().code() != null &&
                    de.getDmsError().code().equals(DmsException.DmsErrorCodes.OBJECT_ARCHIVED.getCode())) {
                log.info("Procedure is already archived: {}", de.getDmsError().message());
            } else {
                log.error("Error while archiving shadow file Procedure {} in File {}", procedureName, fileCoo, e);
            }
        }
    }
}

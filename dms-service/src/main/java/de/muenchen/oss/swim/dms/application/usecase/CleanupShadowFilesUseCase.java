package de.muenchen.oss.swim.dms.application.usecase;

import static de.muenchen.oss.swim.dms.application.usecase.helper.DmsHelper.SHADOW_PROCEDURE_NAME_PATTERN;

import de.muenchen.oss.swim.dms.application.port.in.CleanupShadowFilesInPort;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupShadowFilesUseCase implements CleanupShadowFilesInPort {
    private final SwimDmsProperties swimDmsProperties;
    private final DmsOutPort dmsOutPort;

    @Override
    public void cleanupShadowFiles() {
        log.info("Starting cleaning up shadow files");
        final SwimDmsProperties.ShadowFileCleanup cleanupProperties = swimDmsProperties.getShadowFileCleanup();
        // construct procedure name of last month
        final LocalDate currentDate = LocalDate.now().minusMonths(1);
        final String procedureName = currentDate.format(SHADOW_PROCEDURE_NAME_PATTERN);
        // for each File coo
        for (final String fileCoo : cleanupProperties.getCoos()) {
            log.debug("Processing Procedure {} in File {}", procedureName, fileCoo);
            final DmsTarget fileDmsTarget = new DmsTarget(fileCoo, cleanupProperties.getUsername(), null, null);
            try {
                // search for Procedure
                final Optional<String> procedureCoo = dmsOutPort.getProcedureCooByName(fileDmsTarget, procedureName);
                // archive Procedure if present
                if (procedureCoo.isPresent()) {
                    log.debug("Archiving Procedure {}", procedureCoo);
                    final DmsTarget procedureDmsTarget = new DmsTarget(procedureCoo.get(), fileDmsTarget);
                    dmsOutPort.archiveObject(procedureDmsTarget);
                }
            } catch (final RuntimeException e) {
                log.error("Error while cleaning up shadow file Procedure {} in File {}", procedureName, fileCoo, e);
            }
        }
        log.info("Finished cleaning up shadow files");
    }
}

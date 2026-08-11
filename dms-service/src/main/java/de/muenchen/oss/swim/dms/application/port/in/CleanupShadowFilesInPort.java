package de.muenchen.oss.swim.dms.application.port.in;

import de.muenchen.oss.swim.dms.domain.model.UseCaseType;

public interface CleanupShadowFilesInPort {
    /**
     * Cleanup the shadow files of the previous month created via {@link UseCaseType#SHADOW_FILE}
     */
    void cleanupShadowFiles();
}

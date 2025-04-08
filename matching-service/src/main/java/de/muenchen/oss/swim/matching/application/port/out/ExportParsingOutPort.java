package de.muenchen.oss.swim.matching.application.port.out;

import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ExportParsingOutPort {
    List<DmsInbox> parseCsv(@NotNull InputStream exportContent) throws IOException;
}

package de.muenchen.oss.swim.matching.application.port.out;

import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

public interface ExportParsingOutPort {
    /**
     * Parse uploaded CSV file.
     * Lines which can't be parsed are skipped.
     *
     * @param exportContent The content of the CSV export.
     * @return Parsed content of the csv.
     * @throws CsvParsingException If parsing fails.
     */
    List<DmsInbox> parseCsv(@NotNull InputStream exportContent) throws CsvParsingException;
}

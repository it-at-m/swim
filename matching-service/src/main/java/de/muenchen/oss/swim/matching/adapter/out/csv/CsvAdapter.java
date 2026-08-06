package de.muenchen.oss.swim.matching.adapter.out.csv;

import de.muenchen.oss.swim.matching.application.port.out.ExportParsingOutPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvAdapter implements ExportParsingOutPort {
    private static final char CSV_DELIMITER = ';';

    private final DmsExportMapper dmsExportMapper;

    @Override
    public List<DmsInbox> parseCsv(final InputStream exportContent) throws CsvParsingException {
        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.typedSchemaFor(DmsInboxDTO.class)
                .withHeader()
                .withColumnSeparator(CSV_DELIMITER)
                .withColumnReordering(true);
        try (MappingIterator<DmsInboxDTO> iterator = csvMapper
                .readerFor(DmsInboxDTO.class)
                .with(schema)
                .readValues(exportContent)) {
            final List<DmsInboxDTO> dtos = new ArrayList<>();
            int lines = 0;
            int skippedLines = 0;
            while (iterator.hasNextValue()) {
                lines++;
                try {
                    dtos.add(iterator.nextValue());
                } catch (final JacksonException e) {
                    skippedLines++;
                    log.debug("Failed to parse line {}: {}", lines, e.getMessage());
                }
            }
            if (skippedLines > 0) {
                log.warn("CSV parsing: {} from {} lines couldn't be parsed and were skipped", skippedLines, lines);
            }
            if (dtos.isEmpty() && lines > 0) {
                throw new CsvParsingException(String.format("Failed to parse any records from CSV file with %d lines", lines));
            }
            return dmsExportMapper.fromDtos(dtos);
        } catch (final JacksonException e) {
            throw new CsvParsingException("Error parsing CSV file", e);
        }
    }
}

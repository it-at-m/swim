package de.muenchen.oss.swim.matching.adapter.out.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.oss.swim.matching.application.port.out.ExportParsingOutPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
                } catch (final IOException e) {
                    skippedLines++;
                }
            }
            if (skippedLines > 0) {
                log.warn("CSV parsing: {} from {} lines couldn't be parsed and were skipped", skippedLines, lines);
            }
            return dmsExportMapper.fromDtos(dtos);
        } catch (final IOException e) {
            throw new CsvParsingException("Error parsing CSV file", e);
        }
    }
}

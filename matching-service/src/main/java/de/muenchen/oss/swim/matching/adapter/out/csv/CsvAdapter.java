package de.muenchen.oss.swim.matching.adapter.out.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.oss.swim.matching.application.port.out.ExportParsingOutPort;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CsvAdapter implements ExportParsingOutPort {
    private static final char CSV_DELIMITER = ';';

    private final DmsExportMapper dmsExportMapper;

    /**
     * Parse uploaded csv.
     *
     * @param exportContent The content of the CSV export.
     * @return Parsed content of the csv.
     * @throws IOException If parsing fails.
     */
    @Override
    public List<DmsInbox> parseCsv(final InputStream exportContent) throws IOException {
        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.typedSchemaFor(DmsInboxDTO.class)
                .withHeader()
                .withColumnSeparator(CSV_DELIMITER)
                .withColumnReordering(true);
        try (MappingIterator<DmsInboxDTO> iterator = csvMapper
                .readerFor(DmsInboxDTO.class)
                .with(schema)
                .readValues(exportContent)) {
            final List<DmsInboxDTO> dtos = iterator.readAll();
            return dmsExportMapper.fromDtos(dtos);
        }
    }
}

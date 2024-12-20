package de.muenchen.swim.matching.adapter.in.rest;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.swim.matching.domain.model.ImportReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class RestAdapter {
    private static final char CSV_DELIMITER = ';';

    private final DmsExportMapper dmsExportMapper;
    private final ProcessDmsExportInPort processDmsExportInPort;

    /**
     * Import a dms export for matching.
     *
     * @param file The dms export as csv file.
     * @return Report of the import.
     */
    @Operation(
            security = { @SecurityRequirement(name = "swim-matching-scheme") }
    )
    @PostMapping(
            value = "dms-import",
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @PreAuthorize("hasAuthority(T(de.muenchen.swim.matching.security.Authorities).DMS_IMPORTER)")
    public ResponseEntity<ImportReport> update(@Parameter(required = true) @RequestParam final MultipartFile file) {
        // validate input
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!Objects.equals(file.getContentType(), "text/csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File needs to be a csv");
        }

        try {
            // parse csv
            final List<DmsInboxDTO> inputInboxDtos = parseCsv(file);
            // process input
            final ImportReport importReport = processDmsExportInPort.process(dmsExportMapper.fromDtos(inputInboxDtos));

            return ResponseEntity.ok(importReport);
        } catch (IOException e) {
            log.error("Error while parsing csv", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse uploaded csv.
     *
     * @param file The uploaded csv file.
     * @return Parsed content of the csv.
     * @throws IOException If parsing fails.
     */
    private List<DmsInboxDTO> parseCsv(final MultipartFile file) throws IOException {
        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.typedSchemaFor(DmsInboxDTO.class)
                .withHeader()
                .withColumnSeparator(CSV_DELIMITER)
                .withColumnReordering(true);
        try (MappingIterator<DmsInboxDTO> iterator = csvMapper
                .readerFor(DmsInboxDTO.class)
                .with(schema)
                .readValues(file.getInputStream())) {
            return iterator.readAll();
        }
    }
}

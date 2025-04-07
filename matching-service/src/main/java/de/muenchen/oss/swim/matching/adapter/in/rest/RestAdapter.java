package de.muenchen.oss.swim.matching.adapter.in.rest;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.oss.swim.matching.domain.model.ImportReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
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
    private static final String CSV_EXTENSION = ".csv";
    private static final List<String> CSV_CONTENT_TYPES = List.of("text/csv", "application/vnd.ms-excel");

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
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
    )
    @ApiResponses(
        { @ApiResponse(
                description = "Import successful",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ImportReport.class)
                ),
                responseCode = "200"
        ), @ApiResponse(
                description = "Input CSV file not valid",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE
                ),
                responseCode = "400"
        ) }
    )
    @PreAuthorize("hasAuthority(T(de.muenchen.oss.swim.matching.security.Authorities).DMS_IMPORTER)")
    public ResponseEntity<ImportReport> update(@Parameter(required = true) @RequestParam final MultipartFile file) {
        // validate input
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        final String originalFilename = file.getOriginalFilename();
        final boolean validOriginalFilename = originalFilename == null || originalFilename.endsWith(CSV_EXTENSION);
        final boolean validContentType = CSV_CONTENT_TYPES.contains(file.getContentType());
        if (!validOriginalFilename || !validContentType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File needs to be a csv");
        }

        try {
            // parse csv
            final List<DmsInboxDTO> inputInboxDtos = parseCsv(file);
            // process input
            final ImportReport importReport = processDmsExportInPort.process(dmsExportMapper.fromDtos(inputInboxDtos));

            return ResponseEntity.ok(importReport);
        } catch (final IOException | ConstraintViolationException e) {
            log.error("Error while parsing csv", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input CSV file: " + e.getMessage(), e);
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

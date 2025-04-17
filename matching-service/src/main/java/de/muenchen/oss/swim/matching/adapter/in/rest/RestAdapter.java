package de.muenchen.oss.swim.matching.adapter.in.rest;

import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
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
    private static final String CSV_EXTENSION = ".csv";
    private static final List<String> CSV_CONTENT_TYPES = List.of("text/csv", "application/vnd.ms-excel");

    private final ProcessDmsExportInPort processDmsExportInPort;

    /**
     * Import an DMS export.
     *
     * @param file The DMS export as csv file.
     * @return Report of the import.
     */
    @Operation(
            security = { @SecurityRequirement(name = "swim-matching-scheme") }
    )
    @PostMapping(
            value = "import-dms-export",
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
            // process input
            final ImportReport importReport = processDmsExportInPort.processExport(file.getInputStream());

            return ResponseEntity.ok(importReport);
        } catch (final CsvParsingException | ConstraintViolationException | IOException e) {
            log.error("Error while parsing csv", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger import via DMS.
     *
     * @return Report of the import.
     */
    @Operation(
            security = { @SecurityRequirement(name = "swim-matching-scheme") }
    )
    @PostMapping("trigger-import-via-dms")
    @ApiResponses(
        {
                @ApiResponse(
                        description = "Import via DMS successful",
                        content = @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ImportReport.class)
                        ),
                        responseCode = "200"
                ),
                @ApiResponse(
                        description = "CSV export file in DMS couldn't be parsed",
                        content = @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE
                        ),
                        responseCode = "400"
                )
        }
    )
    @PreAuthorize("hasAuthority(T(de.muenchen.oss.swim.matching.security.Authorities).DMS_IMPORTER)")
    public ResponseEntity<ImportReport> triggerImportViaDms() {
        try {
            final ImportReport importReport = processDmsExportInPort.triggerProcessingViaDms();
            return ResponseEntity.ok(importReport);
        } catch (final CsvParsingException | IOException e) {
            log.error("Error while parsing CSV export in DMS", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error while parsing CSV export in DMS: " + e.getMessage(), e);
        }
    }
}

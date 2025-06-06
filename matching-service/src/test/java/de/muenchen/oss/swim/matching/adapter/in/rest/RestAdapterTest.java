package de.muenchen.oss.swim.matching.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.muenchen.oss.swim.matching.application.port.in.ProcessDmsExportInPort;
import de.muenchen.oss.swim.matching.domain.exception.CsvParsingException;
import de.muenchen.oss.swim.matching.domain.model.ImportReport;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RestAdapterTest {
    private final ProcessDmsExportInPort processDmsExportInPort = mock();
    @Spy
    private final RestAdapter restAdapter = new RestAdapter(processDmsExportInPort);

    @Test
    void testUpdateFileIsEmpty() {
        final MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        final ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> restAdapter.update(emptyFile));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", response.getBody().getDetail());
    }

    @Test
    void testUpdateFileNotCsv() {
        final MultipartFile nonCsvFile = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        final ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> restAdapter.update(nonCsvFile));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File needs to be a csv", response.getBody().getDetail());
    }

    @Test
    void testUpdateSuccessfulProcessing() throws IOException, CsvParsingException {
        final ResponseEntity<ImportReport> response;
        try (InputStream csvInputStream = getClass().getResourceAsStream("/files/test.csv")) {
            final MultipartFile csvFile = new MockMultipartFile("file", "test.csv", "text/csv", csvInputStream);

            response = restAdapter.update(csvFile);
        }

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // test use case call
        verify(processDmsExportInPort, times(1)).processExport(any());
    }

    @Test
    void testTriggerImportViaDmsSuccessful() throws CsvParsingException, IOException {
        final ResponseEntity<ImportReport> response = restAdapter.triggerImportViaDms();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // test use case call
        verify(processDmsExportInPort, times(1)).triggerProcessingViaDms();
    }
}

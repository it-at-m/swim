package de.muenchen.oss.swim.matching.adapter.out.dms;

import de.muenchen.oss.swim.matching.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.matching.domain.exception.DmsException;
import de.muenchen.refarch.integration.dms.api.ContentObjectsApi;
import de.muenchen.refarch.integration.dms.model.ReadContentObjectResponseDTO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DmsAdapter implements DmsOutPort {
    public static final String DMS_EXCEPTION_MESSAGE = "Dms request failed with message: %s";
    private final ContentObjectsApi contentObjectsApi;
    private final DmsProperties dmsProperties;

    private final static String DMS_APPLICATION = "SWIM-Matching";

    @Override
    public InputStream getExportContent() {
        try {
            // FIXME stream response
            final ReadContentObjectResponseDTO response = this.contentObjectsApi.readContentObject(
                    dmsProperties.getImportCoo(),
                    DMS_APPLICATION,
                    dmsProperties.getImportUsername(),
                    null,
                    null).block();
            if (response != null && response.getGiattachmenttype() != null && response.getGiattachmenttype().getFileContent() != null) {
                final byte[] content = response.getGiattachmenttype().getFileContent().getBytes(StandardCharsets.UTF_8);
                return Base64.getDecoder().wrap(new ByteArrayInputStream(content));
            } else {
                throw new DmsException("Invalid response while creating ContentObject in Inbox");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }
}

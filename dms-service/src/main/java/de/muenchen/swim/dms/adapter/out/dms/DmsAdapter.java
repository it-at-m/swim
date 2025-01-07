package de.muenchen.swim.dms.adapter.out.dms;

import de.muenchen.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import de.muenchen.refarch.integration.dms.model.CreateObjectAndImportToInboxDTO;
import de.muenchen.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.swim.dms.domain.exception.DmsException;
import de.muenchen.swim.dms.domain.model.DmsTarget;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DmsAdapter implements DmsOutPort {
    private final ObjectAndImportToInboxApi objectAndImportToInboxApi;
    private final static String DMS_APPLICATION = "SWIM";

    @Override
    public void putFileInInbox(final DmsTarget dmsTarget, final String fileName, final InputStream inputStream) {
        log.debug("Putting file {} in {}", fileName, dmsTarget);
        final CreateObjectAndImportToInboxDTO request = new CreateObjectAndImportToInboxDTO();
        request.setObjaddress(dmsTarget.coo());
        try {
            // FIXME directly use InputStream
            final AbstractResource file = new NamedByteArrayRessource(fileName, inputStream.readAllBytes());
            objectAndImportToInboxApi.createObjectAndImportToInbox(
                    request,
                    DMS_APPLICATION,
                    dmsTarget.userName(),
                    null,
                    null,
                    List.of(file)).block();
        } catch (final IOException | WebClientResponseException e) {
            throw new DmsException("Error while putting file in inbox", e);
        }
    }
}

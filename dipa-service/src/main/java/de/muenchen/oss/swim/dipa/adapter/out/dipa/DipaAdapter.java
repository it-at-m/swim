package de.muenchen.oss.swim.dipa.adapter.out.dipa;

import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.ArrayOfMUCSDIPABAI151700GIAttachmentType;
import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.CreateIncomingGI;
import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.CreateIncomingGIResponse;
import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.MUCSDIPABAI151700GIAttachmentType;
import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.MUCSDIPABAI151700GIWSDSoap;
import de.muenchen.oss.swim.dipa.application.port.out.DipaOutPort;
import de.muenchen.oss.swim.dipa.domain.exception.DipaException;
import de.muenchen.oss.swim.dipa.domain.model.dipa.HrSubfileContext;
import de.muenchen.oss.swim.dipa.domain.model.dipa.IncomingRequest;
import jakarta.activation.DataHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DipaAdapter implements DipaOutPort {
    private static final String APPLICATION = "SWIM";
    private static final int STATE_CODE_SUCCESSFUL = 0;

    private final MUCSDIPABAI151700GIWSDSoap soapClient;

    @Override
    public String createHrSubfileIncoming(final HrSubfileContext context, final IncomingRequest incomingRequest) {
        // build Incoming request
        final CreateIncomingGI request = new CreateIncomingGI();
        request.setUserlogin(context.getUsername());
        request.setFilesubj(incomingRequest.subject());
        request.setBusinessapp(APPLICATION);
        request.setPersnum(context.getPersNr());
        request.setCategory(context.getCategory());
        // build and add ContentObject
        final MUCSDIPABAI151700GIAttachmentType file = new MUCSDIPABAI151700GIAttachmentType();
        file.setMUCSDIPABAI151700Filename(incomingRequest.contentObject().name());
        file.setMUCSDIPABAI151700Filecontent(new DataHandler(new InputStreamDataSource(incomingRequest.contentObject().content())));
        file.setMUCSDIPABAI151700Fileextension(incomingRequest.contentObject().extension());
        final ArrayOfMUCSDIPABAI151700GIAttachmentType files = new ArrayOfMUCSDIPABAI151700GIAttachmentType();
        files.getMUCSDIPABAI151700GIAttachmentType().add(file);
        request.setGiattachmenttype(files);
        // create Incoming
        final CreateIncomingGIResponse response = soapClient.createIncomingGI(request);
        if (response.getStatus() != STATE_CODE_SUCCESSFUL) {
            final String message = String.format("DiPa createHrSubfileIncoming request failed (Code: %d): %s", response.getStatus(),
                    response.getErrormessage());
            log.error(message);
            throw new DipaException(message);
        }
        final String id = response.getObjid();
        log.info("Created Incoming (Id: {}) in HrSubfile (PersNr: {}, Category: {})", id, context.getPersNr(), context.getCategory());
        return response.getObjid();
    }
}

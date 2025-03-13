package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsRequestContext;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.refarch.integration.dms.api.ContentObjectsApi;
import de.muenchen.refarch.integration.dms.api.IncomingsApi;
import de.muenchen.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import de.muenchen.refarch.integration.dms.api.ProcedureObjectsApi;
import de.muenchen.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.refarch.integration.dms.api.SearchObjNamesApi;
import de.muenchen.refarch.integration.dms.model.CreateContentObjectAnfrageDTO;
import de.muenchen.refarch.integration.dms.model.CreateContentObjectAntwortDTO;
import de.muenchen.refarch.integration.dms.model.CreateIncomingAntwortDTO;
import de.muenchen.refarch.integration.dms.model.CreateIncomingBasisAnfrageDTO;
import de.muenchen.refarch.integration.dms.model.CreateObjectAndImportToInboxDTO;
import de.muenchen.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.refarch.integration.dms.model.ReadProcedureObjectsAntwortDTO;
import de.muenchen.refarch.integration.dms.model.ReadProcedureResponseDTO;
import de.muenchen.refarch.integration.dms.model.SearchObjNameAnfrageDTO;
import de.muenchen.refarch.integration.dms.model.SearchObjNameAntwortDTO;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class DmsAdapter implements DmsOutPort {
    public static final String DMS_EXCEPTION_MESSAGE = "Dms request failed with message: %s";
    private final ObjectAndImportToInboxApi objectAndImportToInboxApi;
    private final IncomingsApi incomingsApi;
    private final ProceduresApi proceduresApi;
    private final ProcedureObjectsApi procedureObjectsApi;
    private final ContentObjectsApi contentObjectsApi;
    private final SearchObjNamesApi searchObjNamesApi;

    private final static String DMS_APPLICATION = "SWIM";
    private final static String DMS_OBJECT_TYPE_INBOX = "FSCVGOV@1.1001:Inbox";
    private final static String DMS_OBJECT_TYPE_PROCEDURE = "DEPRECONFIG@15.1001:Procedure";
    private final static Map<DmsResourceType, String> DMS_OBJECT_TYPE_MAPPING = Map.of(
            DmsResourceType.PROCEDURE, DMS_OBJECT_TYPE_PROCEDURE,
            DmsResourceType.INBOX, DMS_OBJECT_TYPE_INBOX);

    @Override
    public void createContentObjectInInbox(final DmsTarget dmsTarget, final String contentObjectName, final InputStream inputStream) {
        log.debug("Putting ContentObject {} in inbox {}", contentObjectName, dmsTarget);
        final CreateObjectAndImportToInboxDTO request = new CreateObjectAndImportToInboxDTO();
        request.setObjaddress(dmsTarget.getCoo());
        try {
            final AbstractResource file = new NamedInputStreamResource(contentObjectName, inputStream);
            objectAndImportToInboxApi.createObjectAndImportToInbox(
                    request,
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    null,
                    null,
                    List.of(file)).block();
            log.info("Created new ContentObject {} in Inbox {}", contentObjectName, dmsTarget);
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String createIncoming(final DmsTarget dmsTarget, final String incomingName, final String incomingSubject, final String contentObjectName,
            final InputStream inputStream) {
        log.debug("Putting file {} in procedure {}", contentObjectName, dmsTarget);
        final CreateIncomingBasisAnfrageDTO request = new CreateIncomingBasisAnfrageDTO();
        request.referrednumber(dmsTarget.getCoo());
        request.shortname(incomingName);
        request.filesubj(incomingSubject);
        request.useou(true);
        try {
            final AbstractResource file = new NamedInputStreamResource(contentObjectName, inputStream);
            final CreateIncomingAntwortDTO response = incomingsApi.createIncoming(
                    request,
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    dmsTarget.getJoboe(),
                    dmsTarget.getJobposition(),
                    List.of(file)).block();
            if (response != null) {
                final String coo = response.getObjid();
                log.info("Created new Incoming {} for {}", coo, dmsTarget);
                return coo;
            } else {
                throw new DmsException("Response null while putting file in procedure");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String getProcedureName(final DmsTarget dmsTarget) {
        try {
            final ReadProcedureResponseDTO response = proceduresApi.readProcedure(
                    dmsTarget.getCoo(),
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    dmsTarget.getJoboe(),
                    dmsTarget.getJobposition()).block();
            if (response != null) {
                final String name = response.getObjname();
                log.info("Found Procedure {} for {}", name, dmsTarget);
                return name;
            } else {
                throw new DmsException("Response null while looking up procedure name");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public Optional<String> getIncomingCooByName(final DmsTarget dmsTarget, final String incomingNamePrefix) {
        try {
            final ReadProcedureObjectsAntwortDTO response = procedureObjectsApi.readProcedureObject(
                    dmsTarget.getCoo(),
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    dmsTarget.getJoboe(),
                    dmsTarget.getJobposition()).block();
            if (response != null && response.getGiobjecttype() != null) {
                return response.getGiobjecttype().stream().filter(
                        i -> i.getName() != null && i.getName().startsWith(incomingNamePrefix)).findFirst().map(Objektreferenz::getId);
            } else {
                throw new DmsException("Response or content null while looking up procedure objects");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String createContentObject(final DmsTarget dmsTarget, final String contentObjectName, final InputStream inputStream) {
        final CreateContentObjectAnfrageDTO createContentObjectAnfrageDTO = new CreateContentObjectAnfrageDTO();
        createContentObjectAnfrageDTO.referrednumber(dmsTarget.getCoo());
        try {
            final AbstractResource file = new NamedInputStreamResource(contentObjectName, inputStream);
            final CreateContentObjectAntwortDTO response = this.contentObjectsApi.createContentObject(
                    createContentObjectAnfrageDTO,
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    dmsTarget.getJoboe(),
                    dmsTarget.getJobposition(),
                    List.of(file)).block();
            if (response != null) {
                final String coo = response.getObjid();
                log.info("Created new ContentObject {} for {}", coo, dmsTarget);
                return coo;
            } else {
                throw new DmsException("Response null while putting file in procedure");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public List<String> findObjectsByName(final DmsResourceType resourceType, final String objectName, final DmsRequestContext requestContext) {
        final SearchObjNameAnfrageDTO request = new SearchObjNameAnfrageDTO();
        request.searchstring(objectName);
        final String dmsObjectType = DMS_OBJECT_TYPE_MAPPING.get(resourceType);
        if (dmsObjectType == null) {
            throw new IllegalArgumentException(String.format("Input resource type %s couldn't be mapped to DMS resource type", resourceType.name()));
        }
        request.setObjclass(dmsObjectType);
        try {
            final SearchObjNameAntwortDTO response = this.searchObjNamesApi.searchObjName(
                    request,
                    DMS_APPLICATION,
                    requestContext.getUsername(),
                    requestContext.getJoboe(),
                    requestContext.getJobposition()).block();
            if (response != null && response.getGiobjecttype() != null) {
                final List<String> coos = response.getGiobjecttype().stream().map(Objektreferenz::getId).toList();
                log.info("Found following coos for {}: {}", objectName, coos);
                return coos;
            } else {
                throw new DmsException("Response or object list null while searching for objects via name");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }
}

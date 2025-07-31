package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsRequestContext;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.refarch.integration.dms.api.ContentObjectsApi;
import de.muenchen.refarch.integration.dms.api.IncomingFromInboxApi;
import de.muenchen.refarch.integration.dms.api.IncomingsApi;
import de.muenchen.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import de.muenchen.refarch.integration.dms.api.ProcedureObjectsApi;
import de.muenchen.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.refarch.integration.dms.api.SearchObjNamesApi;
import de.muenchen.refarch.integration.dms.model.CreateContentObjectAnfrageDTO;
import de.muenchen.refarch.integration.dms.model.CreateContentObjectAntwortDTO;
import de.muenchen.refarch.integration.dms.model.CreateIncomingAntwortDTO;
import de.muenchen.refarch.integration.dms.model.CreateIncomingBasisAnfrageDTO;
import de.muenchen.refarch.integration.dms.model.CreateIncomingFromInboxRequestDTO;
import de.muenchen.refarch.integration.dms.model.CreateObjectAndImportToInboxDTO;
import de.muenchen.refarch.integration.dms.model.CreateObjectAndImportToInboxResponseDTO;
import de.muenchen.refarch.integration.dms.model.DmsObjektResponse;
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
import org.apache.commons.lang3.StringUtils;
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
    private final IncomingFromInboxApi incomingFromInboxApi;
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
    public String createContentObjectInInbox(final DmsTarget dmsTarget, final DmsContentObjectRequest contentObjectRequest,
            final InputStream inputStream) {
        log.debug("Putting ContentObject {} in inbox {}", contentObjectRequest.name(), dmsTarget);
        final CreateObjectAndImportToInboxDTO request = new CreateObjectAndImportToInboxDTO();
        request.setObjaddress(dmsTarget.getCoo());
        if (StringUtils.isNotBlank(contentObjectRequest.subject())) {
            request.setFilesubj(List.of(List.of(contentObjectRequest.subject())));
        }
        try {
            final AbstractResource file = new NamedInputStreamResource(contentObjectRequest.name(), inputStream);
            final CreateObjectAndImportToInboxResponseDTO response = objectAndImportToInboxApi.createObjectAndImportToInbox(
                    request,
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    null,
                    null,
                    List.of(file)).block();
            if (response != null && response.getListcontents() != null && response.getListcontents().size() == 1) {
                final String coo = response.getListcontents().getFirst().getObjaddress();
                log.info("Created new ContentObject {} in Inbox {}", coo, dmsTarget);
                return coo;
            } else {
                throw new DmsException("Invalid response while creating ContentObject in Inbox");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String createIncomingInInbox(final DmsTarget dmsTarget, final DmsIncomingRequest incomingRequest, final InputStream inputStream) {
        log.debug("Putting Incoming {} in inbox {}", incomingRequest.name(), dmsTarget);
        // create ContentObject
        final String contentObjectCoo = this.createContentObjectInInbox(dmsTarget, incomingRequest.contentObject(), inputStream);
        // create Incoming from existing ContentObject
        final CreateIncomingFromInboxRequestDTO request = new CreateIncomingFromInboxRequestDTO();
        request.inboxid(dmsTarget.getCoo());
        request.contentid(contentObjectCoo);
        request.shortname(incomingRequest.name());
        request.filesubj(incomingRequest.subject());
        try {
            final DmsObjektResponse response = incomingFromInboxApi.createIncomingFromInbox(
                    request,
                    DMS_APPLICATION,
                    dmsTarget.getUsername(),
                    dmsTarget.getJoboe(),
                    dmsTarget.getJobposition()).block();
            if (response != null) {
                final String coo = response.getObjid();
                log.info("Created new Incoming {} in Inbox {}", coo, dmsTarget);
                return coo;
            } else {
                throw new DmsException("Response null while creating Incoming in Inbox");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String createProcedureIncoming(final DmsTarget dmsTarget, final DmsIncomingRequest incomingRequest, final InputStream inputStream) {
        log.debug("Putting Incoming {} in Procedure {}", incomingRequest.name(), dmsTarget);
        final CreateIncomingBasisAnfrageDTO request = new CreateIncomingBasisAnfrageDTO();
        request.referrednumber(dmsTarget.getCoo());
        request.shortname(incomingRequest.name());
        request.filesubj(incomingRequest.subject());
        request.useou(true);
        try {
            final AbstractResource file = new NamedInputStreamResource(incomingRequest.contentObject().name(), inputStream);
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
                final List<Objektreferenz> matchingIncomings = response.getGiobjecttype().stream().filter(
                        i -> i.getObjname() != null && i.getObjname().startsWith(incomingNamePrefix))
                        .toList();
                if (matchingIncomings.size() > 1) {
                    log.warn("Using first of multiple matching Incomings with prefix {} for {}", incomingNamePrefix, dmsTarget);
                }
                return Optional.ofNullable(matchingIncomings.getFirst().getObjaddress());
            } else {
                throw new DmsException("Response or content null while looking up procedure objects");
            }
        } catch (final WebClientResponseException e) {
            throw new DmsException(String.format(DMS_EXCEPTION_MESSAGE, e.getResponseBodyAsString()), e);
        }
    }

    @Override
    public String createContentObject(final DmsTarget dmsTarget, final DmsContentObjectRequest contentObjectRequest, final InputStream inputStream) {
        final CreateContentObjectAnfrageDTO createContentObjectAnfrageDTO = new CreateContentObjectAnfrageDTO();
        createContentObjectAnfrageDTO.referrednumber(dmsTarget.getCoo());
        try {
            final AbstractResource file = new NamedInputStreamResource(contentObjectRequest.name(), inputStream);
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
                final List<String> coos = response.getGiobjecttype().stream().map(Objektreferenz::getObjaddress).toList();
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

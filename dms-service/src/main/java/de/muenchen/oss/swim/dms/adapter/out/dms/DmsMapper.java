package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsErrorResponse;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface DmsMapper {
    @Mapping(source = "status", target = "code")
    @Mapping(source = "text", target = "message")
    @Mapping(source = "fehlerQuelle", target = "source")
    DmsException.DmsError toDomain(DmsErrorResponse dmsErrorResponse);
}

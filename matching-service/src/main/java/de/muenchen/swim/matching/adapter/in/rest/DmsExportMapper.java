package de.muenchen.swim.matching.adapter.in.rest;

import de.muenchen.swim.matching.domain.model.DmsInbox;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DmsExportMapper {
    @Mapping(source = "lhmObjectId", target = "ownerLhmObjectId")
    @Mapping(source = "mandant", target = "dmsTenant")
    DmsInbox fromDto(DmsInboxDTO dmsInboxDto);

    List<DmsInbox> fromDtos(List<DmsInboxDTO> dmsInboxDtos);

    default String mapEmptyStringToNull(final String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}

package de.muenchen.oss.swim.dispatcher.adapter.out.db;

import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface DbProtocolMapper {
    @Mapping(target = "id", ignore = true)
    DbProtolEntry toDb(String useCase, String protocolName, ProtocolEntry entry);

    default List<DbProtolEntry> toDb(final String useCase, final String protocolName, final List<ProtocolEntry> entries) {
        return entries.stream().map(i -> toDb(useCase, protocolName, i)).toList();
    }
}

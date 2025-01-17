package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface ProtocolMapper {
    /**
     * Convert a raw entry extracted from the csv to the according domain model.
     *
     * @param csvProtocolEntryList List of csv entries.
     * @return Same list represented by domain models.
     */
    List<ProtocolEntry> toDomain(List<CsvProtocolEntry> csvProtocolEntryList);
}

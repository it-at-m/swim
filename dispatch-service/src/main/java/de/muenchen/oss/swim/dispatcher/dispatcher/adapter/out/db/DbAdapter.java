package de.muenchen.oss.swim.dispatcher.dispatcher.adapter.out.db;

import de.muenchen.oss.swim.dispatcher.dispatcher.application.port.out.StoreProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.model.protocol.ProtocolEntry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbAdapter implements StoreProtocolOutPort {
    private final ProtocolRepository protocolRepository;
    private final DbProtocolMapper dbProtocolMapper;

    @Override
    public void storeProtocol(final String useCase, final String protocolName, final List<ProtocolEntry> entries) {
        protocolRepository.saveAll(dbProtocolMapper.toDb(useCase, protocolName, entries));
    }
}

package de.muenchen.oss.swim.dispatcher.adapter.out.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
interface ProtocolRepository extends JpaRepository<DbProtocolEntry, String> {
    @Transactional
    @Modifying
    void deleteAllByUseCaseAndProtocolName(String useCase, String protocolName);
}

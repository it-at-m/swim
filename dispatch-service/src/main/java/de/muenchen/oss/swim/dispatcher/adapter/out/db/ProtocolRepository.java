package de.muenchen.oss.swim.dispatcher.adapter.out.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ProtocolRepository extends JpaRepository<DbProtolEntry, String> {
}

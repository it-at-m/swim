package de.muenchen.oss.swim.matching.adapter.out.db;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressFBWarnings("EI_EXPOSE_REP2")
class UserInboxRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final int CHUNK_SIZE = 100;

    protected void deleteByDmsTenant(final String dmsTenant) {
        final String sql = String.format("DELETE FROM %s WHERE dms_tenant = ?", UserInboxMatchingEntry.TABLE_NAME);
        jdbcTemplate.update(sql, dmsTenant);
    }

    protected void saveAll(final List<UserInboxMatchingEntry> userInboxMatchingEntries) {
        final String sql = String.format(
                "INSERT INTO %s (id, coo, inbox_name, username, firstname, surname, ou, street, postal_code, city, dms_tenant) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UserInboxMatchingEntry.TABLE_NAME);
        jdbcTemplate.batchUpdate(sql, userInboxMatchingEntries, CHUNK_SIZE, (ps, argument) -> {
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID().toString());
            }
            ps.setString(1, argument.getId());
            ps.setString(2, argument.getCoo());
            ps.setString(3, argument.getInboxName());
            ps.setString(4, argument.getUsername());
            ps.setString(5, argument.getFirstname());
            ps.setString(6, argument.getSurname());
            ps.setString(7, argument.getOu());
            ps.setString(8, argument.getStreet());
            ps.setString(9, argument.getPostalCode());
            ps.setString(10, argument.getCity());
            ps.setString(11, argument.getDmsTenant());
        });
    }
}

package de.muenchen.oss.swim.matching.adapter.out.db;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class GroupInboxRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final int CHUNK_SIZE = 100;

    protected void deleteByDmsTenant(final String dmsTenant) {
        final String sql = String.format("DELETE FROM %s WHERE dms_tenant = ?", GroupInboxMatchingEntry.TABLE_NAME);
        jdbcTemplate.update(sql, dmsTenant);
    }

    protected void saveAll(final List<GroupInboxMatchingEntry> groupInboxMatchingEntries) {
        final String sql = String.format("INSERT INTO %s (coo, username, inbox_name, ou, dms_tenant) VALUES (?, ?, ?, ?, ?)",
                GroupInboxMatchingEntry.TABLE_NAME);
        jdbcTemplate.batchUpdate(sql, groupInboxMatchingEntries, CHUNK_SIZE, (ps, argument) -> {
            ps.setString(1, argument.getCoo());
            ps.setString(2, argument.getUsername());
            ps.setString(3, argument.getInboxName());
            ps.setString(4, argument.getOu());
            ps.setString(5, argument.getDmsTenant());
        });
    }
}

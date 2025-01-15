package de.muenchen.oss.swim.matching.adapter.out.ldap;

import de.muenchen.oss.swim.matching.application.port.out.UserInformationOutPort;
import de.muenchen.oss.swim.matching.domain.model.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ldap.core.LdapTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class LdapAdapter implements UserInformationOutPort {
    public static final String CACHE_KEY = "ldap-users";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String OBJECT_CLASS_PERSON = "lhmPerson";
    public static final String LHM_OBJECT_ID = "lhmObjectID";

    private final LdapTemplate ldapTemplate;
    private final LdapProperties ldapProperties;

    @Override
    @Cacheable(CACHE_KEY)
    public List<User> getAllUsers() {
        log.debug("Starting loading users from ldap");
        final List<User> users = new ArrayList<>();
        for (final String ou : ldapProperties.getSearchOus()) {
            users.addAll(this.getUsersFromOu(ou));
        }
        log.info("Finished loading {} users from ldap", users.size());
        return users;
    }

    protected List<User> getUsersFromOu(final String ou) {
        final String base = String.format(ldapProperties.getUserBaseOu(), ou);
        // build query
        final LdapQuery query = LdapQueryBuilder.query()
                .base(base)
                .attributes(LhmPersonAttributeMapper.getRequiredAttributes())
                .where(OBJECT_CLASS).is(OBJECT_CLASS_PERSON)
                .and(LHM_OBJECT_ID).isPresent();
        // search
        return ldapTemplate.search(query, new LhmPersonAttributeMapper());
    }
}

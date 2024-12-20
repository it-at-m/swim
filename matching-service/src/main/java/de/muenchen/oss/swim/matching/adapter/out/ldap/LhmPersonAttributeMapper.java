package de.muenchen.oss.swim.matching.adapter.out.ldap;

import de.muenchen.oss.swim.matching.domain.model.Address;
import de.muenchen.oss.swim.matching.domain.model.User;
import java.util.Arrays;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.springframework.ldap.core.AttributesMapper;

public class LhmPersonAttributeMapper implements AttributesMapper<User> {
    private static final String[] REQUIRED_ATTRIBUTES = {
            "lhmObjectID", "uid", "givenName", "sn", "ou",
            "lhmOfficeStreetAddress", "lhmOfficePostalCode", "lhmOfficeLocalityName",
            "street", "postalCode", "l"
    };

    protected static String[] getRequiredAttributes() {
        return Arrays.copyOf(REQUIRED_ATTRIBUTES, REQUIRED_ATTRIBUTES.length);
    }

    @Override
    public User mapFromAttributes(final Attributes attrs) throws NamingException {
        final Address officeAddress = new Address(
                getStringAttributeOrNull(attrs, "lhmOfficeStreetAddress"),
                getStringAttributeOrNull(attrs, "lhmOfficePostalCode"),
                getStringAttributeOrNull(attrs, "lhmOfficeLocalityName"));
        final Address postalAddress = new Address(
                getStringAttributeOrNull(attrs, "street"),
                getStringAttributeOrNull(attrs, "postalCode"),
                getStringAttributeOrNull(attrs, "l"));
        return new User(
                attrs.get("lhmObjectID").get().toString(),
                attrs.get("uid").get().toString(),
                getStringAttributeOrNull(attrs, "givenName"),
                getStringAttributeOrNull(attrs, "sn"),
                getStringAttributeOrNull(attrs, "ou"),
                officeAddress,
                postalAddress);
    }

    private String getStringAttributeOrNull(final Attributes attributes, final String attr) throws NamingException {
        final Attribute attribute = attributes.get(attr);
        if (attribute != null) {
            return attribute.get().toString();
        }
        return null;
    }
}

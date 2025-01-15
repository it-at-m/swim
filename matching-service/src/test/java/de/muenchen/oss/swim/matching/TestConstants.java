package de.muenchen.oss.swim.matching;

import de.muenchen.oss.swim.matching.domain.model.Address;
import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import de.muenchen.oss.swim.matching.domain.model.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({ "PMD.TestClassWithoutTestCases", "PMD.DataClass" })
public final class TestConstants {

    public static final String SPRING_TEST_PROFILE = "test";

    public static final String SPRING_NO_SECURITY_PROFILE = "no-security";

    public static final String SPRING_JSON_LOGGING_PROFILE = "json-logging";

    public static final String TESTCONTAINERS_POSTGRES_IMAGE = "postgres:16.0-alpine3.18";

    public static final String TENANT1 = "DPF";
    public static final String OU1_1 = "DPF-123";
    public static final String OU1_2 = "DPF-321";
    public static final String TENANT2 = "DPS";
    public static final String OU2_1 = "DPS-123";
    public static final DmsInbox USER_INBOX_1 = new DmsInbox(
            "COO.1234.1234.1.1234567",
            "Test User Inbox",
            "123456789",
            OU1_1,
            TENANT1,
            DmsInbox.InboxType.USER);
    public static final DmsInbox USER_INBOX_2 = new DmsInbox(
            "COO.1234.1234.1.7654321",
            "Test User Inbox 2",
            "987654321",
            OU2_1,
            TENANT2,
            DmsInbox.InboxType.USER);
    public static final DmsInbox GROUP_INBOX_1 = new DmsInbox(
            "COO.1234.1234.2.1234567",
            "Test Group Inbox",
            "123456789",
            OU1_2,
            TENANT1,
            DmsInbox.InboxType.GROUP);
    public static final User USER_1 = new User(
            "123456789",
            "f.surname",
            "firstname",
            "surname",
            OU1_1,
            new Address("streetOffice", "postalcodeOffice", "cityOffice"),
            new Address("streetPostal", "postalcodePostal", "cityPostal"));
    public static final User USER_2 = new User(
            "987654321",
            "f.surname2",
            "firstname2",
            "surname2",
            OU2_1,
            new Address("streetOffice2", "postalcodeOffice2", "cityOffice2"),
            new Address("streetPostal2", "postalcodePostal2", "cityPostal2"));
}

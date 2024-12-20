package de.muenchen.swim.matching.application.port.out;

import de.muenchen.swim.matching.domain.model.User;
import java.util.List;

public interface UserInformationOutPort {
    /**
     * Get all users available for user inbox enriching.
     *
     * @return List of users to be used for user inbox enriching.
     */
    List<User> getAllUsers();
}

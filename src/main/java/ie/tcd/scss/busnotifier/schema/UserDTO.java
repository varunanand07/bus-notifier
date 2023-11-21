package ie.tcd.scss.busnotifier.schema;

import ie.tcd.scss.busnotifier.domain.User;

public class UserDTO {
    public String username;
    public String firstname;
    public String lastname;

    public UserDTO(User user) {
        this.username = user.getUsername();
        this.firstname = user.getFirstname();
        this.lastname = user.getLastname();
    }
}

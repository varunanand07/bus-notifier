package ie.tcd.scss.busnotifier.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequestDTO {
    public String firstname;
    public String lastname;
    public String username;
    public String password;
}
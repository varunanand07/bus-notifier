package ie.tcd.scss.busnotifier.controller;

import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.schema.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class UserController {
    @GetMapping("/user")
    public ResponseEntity<Object> getUser(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(new UserDTO(user));
    }
}

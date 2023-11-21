package ie.tcd.scss.busnotifier.controller;

import ie.tcd.scss.busnotifier.exceptions.UserExists;
import ie.tcd.scss.busnotifier.schema.AuthenticationResponseDTO;
import ie.tcd.scss.busnotifier.schema.GenericErrorDTO;
import ie.tcd.scss.busnotifier.schema.LoginRequestDTO;
import ie.tcd.scss.busnotifier.schema.RegisterRequestDTO;
import ie.tcd.scss.busnotifier.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterRequestDTO request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (UserExists e) {
            var error = new GenericErrorDTO(
                    "user-already-exists",
                    String.format("User `%s` already exists", request.getUsername())
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> register(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
package ie.tcd.scss.busnotifier.service;

import ie.tcd.scss.busnotifier.domain.Role;
import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.exceptions.UserExists;
import ie.tcd.scss.busnotifier.repo.UserRepository;
import ie.tcd.scss.busnotifier.schema.AuthenticationResponseDTO;
import ie.tcd.scss.busnotifier.schema.LoginRequestDTO;
import ie.tcd.scss.busnotifier.schema.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private final  AuthenticationManager authenticationManager;

    @Autowired
    private final JwtService jwtService;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    /**
     * Tries to create a user in the database and then returns an authentication token for the newly created
     * user.
     * @param registerRequest The parameters used for registration
     * @return An authentication token
     * @throws UserExists if the username specified already exists
     */
    public AuthenticationResponseDTO register(RegisterRequestDTO registerRequest) throws UserExists {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            logger.error(String.format("Refusing to recreate existing user `%s`", registerRequest.getUsername()));
            throw new UserExists("User already exists");
        }
        var user = User.builder()
                .firstname(registerRequest.getFirstname())
                .lastname(registerRequest.getLastname())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        var token = jwtService.generateToken(user);
        return new AuthenticationResponseDTO(token);
    }

    /**
     * Tries to authenticate a user using the parameters in the login request.
     * @param loginRequest The parameters to be used for logging in
     * @return An authentication token
     */
    public AuthenticationResponseDTO login(LoginRequestDTO loginRequest) {
         var authentication = authenticationManager.authenticate(
                 new UsernamePasswordAuthenticationToken(
                         loginRequest.getUsername(),
                         loginRequest.getPassword()
                 )
         );
         var token = jwtService.generateToken((User) authentication.getPrincipal());
         return new AuthenticationResponseDTO(token);
    }
}
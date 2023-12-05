package ie.tcd.scss.busnotifier.config;

import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter  {

    @Autowired
    private UserDetailsService userDetailsService;

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        var noAuthHeader = authHeader == null;
        var invalidAuthHeader = authHeader != null && !authHeader.startsWith("Bearer ");
        if (noAuthHeader || invalidAuthHeader) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);
        var username = jwtService.extractAllClaims(token).getSubject();

        var noUsername = username == null;
        var authExistsAlready = SecurityContextHolder.getContext().getAuthentication() != null;
        if (noUsername || authExistsAlready) {
            filterChain.doFilter(request, response);
            return;
        }

        User user;

        try {
            user = (User) userDetailsService.loadUserByUsername(username);
            user.browserEndpoints = null;
            user.dublinBusSubscriptions = null;
        } catch (UsernameNotFoundException e) {
            System.out.printf("Rejecting a user `%s`, not found\n", username);
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validate(token, user)) {
            filterChain.doFilter(request, response);
            return;
        }
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        authenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}
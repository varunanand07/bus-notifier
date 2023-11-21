package ie.tcd.scss.busnotifier.config;

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
        System.out.printf("My authorization header is: `%s`\n", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            System.out.println("HERE I AM #0");
            return;
        }
        var token = authHeader.substring(7);
        var username = jwtService.extractAllClaims(token).getSubject();

        if (username == null) {
            System.out.println("HERE I AM #1");
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("HERE I AM #3");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.validate(token, userDetails)) {
                System.out.println("HERE I AM #2");
                filterChain.doFilter(request, response);
                return;
            }
            var authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (UsernameNotFoundException e) {
            System.out.printf("Rejecting a user `%s`, not found\n", username);
            filterChain.doFilter(request, response);
        }
    }
}
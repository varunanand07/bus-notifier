package ie.tcd.scss.busnotifier.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.signing.key}")
    private String signingKeyBase64;

    @Getter(lazy = true)
    private final Key signingKey = decodeSigningKey();

    /**
     * Checks if a JWT is valid for a particular user
     * @param token The dot-separated base64 encoded JWT
     * @param userDetails The user we are validating against
     * @return true if valid, false otherwise
     */
    public boolean validate(String token, UserDetails userDetails) {
        try {
            return extractAllClaims(token)
                    .get("sub", String.class)
                    .equals(userDetails.getUsername());
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    /**
     * Generate a token that authenticates a particular user
     * @param userDetails The user you want to authenticate
     * @return The signed dot-separated base64 encoded JWT
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts
                .builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5 ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the claims in the body of this JWT. Note that this will reject expired JWTs.
     * @param token The dot-separated base64 encoded JWT
     * @return The claims encoded in the JWT
     */
    public Claims extractAllClaims(String token) {
        return (Claims) Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parse(token)
                .getBody();
    }

    private Key decodeSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingKeyBase64));
    }
}

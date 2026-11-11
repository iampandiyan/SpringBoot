package com.example.trade.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// A tiny "login" endpoint: the caller proves who they are with HTTP Basic (see tokenChain), we hand back a signed JWT.
// This stands in for an identity provider (Keycloak, Entra ID, Okta...). In a real system THEY issue the tokens
// and this service only validates them, so this controller would simply not exist.
@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)   // needs the JwtEncoder from SecurityConfig
public class TokenController {

    // The OAuth2 field names are snake_case ("access_token", "expires_in"), so we name them explicitly.
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn) {}

    private final JwtEncoder encoder;
    private final SecurityProperties props;

    public TokenController(JwtEncoder encoder, SecurityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    @PostMapping("/auth/token")
    public TokenResponse token(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .sorted()
                .toList();

        // Claims = the facts inside the token: who (sub), who issued it (iss), validity window (iat, exp), and roles.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("trade-service")
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(props.tokenTtl()))
                .claim("roles", roles)
                .build();

        String jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(jwt, "Bearer", props.tokenTtl().toSeconds());
    }
}

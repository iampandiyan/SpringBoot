package com.example.trade.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// The whole security setup in one class. Two filter chains, because two different jobs need two different rules:
//   1. /auth/**  : "log in" with a username and password (HTTP Basic) and receive a JWT.
//   2. everything else : the API, which accepts ONLY a valid JWT (Authorization: Bearer ...). No sessions, no cookies.
//
// @ConditionalOnWebApplication: HttpSecurity only exists in a web application. Without this line the non-web run from
// lesson 01 (--spring.main.web-application-type=none) and our non-web tests would fail at startup.
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity           // turns on @PreAuthorize on controller/service methods
public class SecurityConfig {

    // The first chain whose securityMatcher matches the request wins, so @Order matters.
    @Bean
    @Order(1)
    SecurityFilterChain tokenChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/auth/**")
                .csrf(csrf -> csrf.disable())       // no browser session or cookie here, so CSRF does not apply
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())           // CSRF protects cookie sessions; a bearer-token API has none
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // Rules are checked top to bottom: the FIRST match wins, so put specific rules first.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()   // load balancers and probes
                        .requestMatchers("/actuator/**").hasRole("ADMIN")                      // beans, env, metrics...
                        .anyRequest().authenticated())                                           // secure by default
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }

    // Maps the "roles" claim of the token (["TRADER"]) to Spring authorities (ROLE_TRADER),
    // so hasRole('TRADER') and @PreAuthorize work.
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // Demo users, held in memory. A real system uses a database, LDAP, or an identity provider.
    // The passwords are stored as hashes (bcrypt via the delegating encoder), never as plain text.
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password(encoder.encode("password")).roles("TRADER").build(),
                User.withUsername("bob").password(encoder.encode("password")).roles("OPS").build(),
                User.withUsername("carol").password(encoder.encode("password")).roles("VIEWER").build(),
                User.withUsername("admin").password(encoder.encode("password")).roles("ADMIN").build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // bcrypt by default, upgradeable
    }

    // --- JWT signing: HS256 = HMAC with ONE shared secret (same key signs and verifies). ---
    // Fine for one service. With several services, use an identity provider and RS256/ES256 public keys (see the lesson).
    @Bean
    SecretKey jwtKey(SecurityProperties props) {
        return new SecretKeySpec(props.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // Defining this bean makes Boot back off from its own JwtDecoder (which would need an issuer-uri).
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }
}

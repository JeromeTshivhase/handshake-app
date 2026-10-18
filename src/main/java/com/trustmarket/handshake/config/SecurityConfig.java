package com.trustmarket.handshake.config;

import com.trustmarket.handshake.shared.security.JwtAuthenticationFilter;
import com.trustmarket.handshake.shared.security.UserPrincipal;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth
                        .requestMatchers("/api/auth/**").permitAll()

                        // Moderation report submission is authenticated; admin report management is restricted below.
                        .requestMatchers(HttpMethod.POST, "/api/moderation/reports").authenticated()
                        .requestMatchers("/api/moderation/reports/**").hasRole("ADMIN")

                        // PayFast ITN must be public
                        .requestMatchers("/api/payments/payfast/notify").permitAll()

                        // Health — public for load-balancer probes only
                        .requestMatchers("/actuator/health", "/api/health").permitAll()
                        // All other actuator endpoints require ADMIN
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Swagger/OpenAPI — only accessible when SWAGGER_ENABLED=true (springdoc disables
                        // the endpoints via config, but we also restrict here as defence-in-depth)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/v3/api-docs/swagger-config",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).hasRole("ADMIN")

                        // Public browsing
                        .requestMatchers(HttpMethod.GET, "/api/listings/**", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/listings/search", "/api/search").permitAll()

                        // Serve uploaded images publicly
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(u -> new UserPrincipal(
                        u.getId(),
                        u.getEmail(),
                        u.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

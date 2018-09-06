package voychris.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationFilter = new AuthenticationWebFilter(new FooReactiveAuthenticationManager());
        authenticationFilter.setAuthenticationConverter(new FooAuthenticationConverter());

        return http
            .csrf().disable()
            .logout().disable()
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
            .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange()
            .anyExchange().access(new FooReactiveAuthorizationManager("can-foo"))
            .and().build();
    }
}

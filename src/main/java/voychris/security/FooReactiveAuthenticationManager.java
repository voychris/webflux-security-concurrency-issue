package voychris.security;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;

public class FooReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final Scheduler scheduler = Schedulers.parallel();

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)

            .publishOn(this.scheduler)// <--------   Offending line of code. Removing this avoids the issue.

            .map((it) -> (FooAuthentication) it)

            .filter((it) -> it.getRawCredentials() != null && !it.getRawCredentials().trim().isEmpty())
            .switchIfEmpty(Mono.defer(() -> Mono.error(new InsufficientAuthenticationException("Missing Permissions."))))

            .map(fooAuthentication -> doAuthenticate(fooAuthentication))
            .onErrorMap(e -> new InternalAuthenticationServiceException("Unable to create success authentication.", e));
    }

    private Authentication doAuthenticate(FooAuthentication fooAuthentication) {

        String rawCredentials = fooAuthentication.getRawCredentials();

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(rawCredentials));

        return new FooAuthentication("bar", authorities);
    }
}

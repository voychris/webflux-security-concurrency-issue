package voychris.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class FooReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final String requiredAuthority;

    FooReactiveAuthorizationManager(String requiredAuthority) {
        this.requiredAuthority = requiredAuthority;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {

        return authentication.flatMapMany(auth -> Flux.fromIterable(auth.getAuthorities()))
            .filter(grantedAuthority -> grantedAuthority.getAuthority().equals(requiredAuthority))
            .collectList().map(list -> new AuthorizationDecision(!list.isEmpty()));
    }
}

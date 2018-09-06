package voychris.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.regex.Pattern;

public class FooAuthenticationConverter implements Function<ServerWebExchange, Mono<Authentication>> {

    @Override
    public Mono<Authentication> apply(ServerWebExchange serverWebExchange) {
        ServerHttpRequest request = serverWebExchange.getRequest();

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            return Mono.just(new FooAuthentication("bar", (String)null));
        }

        String[] parts = authorization.split(" ");

        if (parts.length != 2) {
            return Mono.just(new FooAuthentication("bar", (String)null));
        }

        String scheme = parts[0];
        String credentials = parts[1];

        Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

        if (pattern.matcher(scheme).matches()) {
            return Mono.just(new FooAuthentication("bar", credentials));
        }

        return Mono.just(new FooAuthentication("bar", (String)null));
    }
}

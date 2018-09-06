# webflux-security-concurrency-issue

### Description

This project shows a concurrency problem I've stumbled upon when using webflux & reactive spring security.
The sample app has a controller (`SimpleController`) and a security filter chain which uses a custom 
`FooReactiveAuthenticationManager` & a `FooAuthenticationConverter`.

When hitting the api with enough concurrency the body of the request gets corrupted. For example, if I hit the api
with 20k requests containing `{"foo": "yyy"}` in the body and another 20k requests containing `{"foo": "xxx"}` and log
the body in the controller I occasionally get something like `xxy`, `xyx`, `yyx` and all the other permutations of `xxx`
and `yyy`.

**Notes:**
- using `RouterFunction` bean instead of `RestController` doesn't make a difference
- changing the controller method signature from `public Mono<Void> updateFoo(@RequestBody Mono<Request> request)` to 
`public Mono<Void> updateFoo(@RequestBody Request request)` doesn't make a difference.
- changing the controller method signature to `public Mono<Void> updateFoo(@RequestBody String request)` i.e. making the request body a simple `String`
avoids the issue i.e. the body doesn't get corrupted.
- commenting out `.publishOn(this.scheduler)` from `FooReactiveAuthenticationManager` avoids the issue.

```java
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
```

### Reproducing

1. Run the app: `./gradlew bootRun`

2. Run the below commands in two separate terminals simultaneously. You can use Apache Bench (ab) or [hey](https://github.com/rakyll/hey). 
With enough concurrency you'll be able to reproduce the issue. I've set the concurrency to 100 but you might be able to lower it.
    ```
    hey -m PUT -d '{"foo": "yyy"}' -H "Authorization: Bearer can-foo" -n 20000 -c 100 -T application/json http://localhost:8080/api/foo
    ```
    
    ```
    hey -m PUT -d '{"foo": "xxx"}' -H "Authorization: Bearer can-foo" -n 20000 -c 100 -T application/json http://localhost:8080/api/foo
    ```
    
    The only difference in the above is the payload. One carries `foo: xxx`, one `foo: yyy`.

3. Observe that the request body in the app gets scrambled:

```
ERROR 73367 --- [ctor-http-nio-1] voychris.api.SampleController : foo: xyy
ERROR 73367 --- [ctor-http-nio-2] voychris.api.SampleController : foo: xyy
ERROR 73367 --- [ctor-http-nio-1] voychris.api.SampleController : foo: yxx
ERROR 73367 --- [ctor-http-nio-4] voychris.api.SampleController : foo: yxx
ERROR 73367 --- [ctor-http-nio-3] voychris.api.SampleController : foo: yxx
...
```

### Issue raised

- [Spring Boot](https://github.com/spring-projects/spring-boot/issues/14315)
- [Spring SPR-17193](https://jira.spring.io/browse/SPR-17193)

### Workaround

The workaround is to disable `USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING` in Jackson. Here's an example:

```java
@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper = new ObjectMapper(
        new JsonFactory().disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));

    @Bean
    public CodecCustomizer codecCustomizer() {
        return configurer -> {
            configurer.defaultCodecs().jackson2JsonDecoder(
                new Jackson2JsonDecoder(objectMapper)
            );

            configurer.defaultCodecs().jackson2JsonEncoder(
                new Jackson2JsonEncoder(objectMapper)
            );
        };
    }
}
```
package voychris.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import voychris.api.dtos.Request;

import java.util.Arrays;

@RestController
public class SampleController {

    private final Logger log = LoggerFactory.getLogger(SampleController.class);

    @PutMapping("/api/foo")
    public Mono<Void> updateFoo(@RequestBody Mono<Request> request) {

        return request.flatMap(req -> {
            if (!Arrays.asList("xxx", "yyy").contains(req.getFoo())) {
                log.error("foo: " + req.getFoo());
            }

            return Mono.empty();
        });
    }
}

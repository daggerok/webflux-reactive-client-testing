package com.github.daggerok;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static lombok.AccessLevel.PROTECTED;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(staticName = "of")
class Event {
  private long id;
  private ZonedDateTime when;

  static Event produce() {
    return Event.of(System.currentTimeMillis(), ZonedDateTime.now());
  }
}

@RestController
@SpringBootApplication
public class WebfluxReactiveClientApplication {

  @RequestMapping("/events/{id}")
  public Mono<Event> getNyId(@PathVariable("id") Long id) {
    return Mono.just(Event.of(id, ZonedDateTime.now()));
  }

  @RequestMapping(path = "/**", produces = TEXT_EVENT_STREAM_VALUE)
  public Flux<Event> eventStream() {
    return Flux.zip(Flux.interval(Duration.ofSeconds(3)),
                    Flux.fromStream(Stream.generate(Event::produce)))
               .map(Tuple2::getT2);
  }

  public static void main(String[] args) {
    SpringApplication.run(WebfluxReactiveClientApplication.class, args);
  }

}

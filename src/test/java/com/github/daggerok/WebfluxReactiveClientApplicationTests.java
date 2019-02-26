package com.github.daggerok;

import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class WebfluxReactiveClientApplicationTests {

  @LocalServerPort
  Integer port;

  static final Function<Integer, WebClient> restClient = port ->
      WebClient.builder().baseUrl(format("http://127.0.0.1:%s", port))
               .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
               .build();

  @Test
  public void should_get_event_stream_by_multiple_clients() throws Exception {
    RequestHeadersSpec<?> spec = restClient.apply(port)
                                           .get()
                                           .uri("/")
                                           .accept(TEXT_EVENT_STREAM);
    spec.retrieve()
        .bodyToFlux(Event.class)
        .map(Event::toString)
        .subscribe(event -> log.info("ev: {}", event));
    WebClient.create(format("http://127.0.0.1:%s", port))
             .get()
             .uri("/")
             .accept(TEXT_EVENT_STREAM)
             .retrieve()
             .bodyToFlux(Event.class)
             .map(Event::toString)
             .map(s -> "ev2: " + s)
             .subscribe(log::info);
    spec.exchange()
        .map(clientResponse -> clientResponse.bodyToFlux(Event.class)
                                             .map(String::valueOf)
                                             .subscribe(log::info))
        .subscribe();
    Thread.sleep(9000);
  }

  @Test
  public void should_get_event_by_id() {
    long randomNumber = new Random().nextLong();
    long positiveNumber = randomNumber < 0 ? -randomNumber : randomNumber;
    RequestBodySpec spec = restClient.apply(port)
                                     .method(GET)
                                     .uri(format("/events/%d", positiveNumber));
    Map response1 = spec.exchange()
                        .flatMap(clientResponse -> clientResponse.bodyToMono(Map.class))
                        .block(); // or use WEbTestClient instead
    Event response2 = spec.exchange()
                          .flatMap(clientResponse -> clientResponse.bodyToMono(Event.class))
                          .block();

    assertThat(response1.get("id")).isNotNull();
    assertThat(response2.getId()).isNotNull();
    assertThat(response1.get("id")).isEqualTo(response2.getId())
                                   .isEqualTo(positiveNumber);

    assertThat(response1.get("when")).isNotNull();
    assertThat(response2.getWhen()).isNotNull();
    assertThat(response1.get("when")).isNotEqualTo(response2.getWhen());
    assertThat(response2.getWhen()).isBeforeOrEqualTo(ZonedDateTime.now());

    log.info("resp 1: {}", response1);
    log.info("resp 2: {}", response2);
  }

  @Test
  public void should_create_webClient_and_get_event_by_id() {
    long randomNumber = new Random().nextLong();
    long positiveNumber = randomNumber < 0 ? -randomNumber : randomNumber;
    WebClient webClient = WebClient.builder()
                                   .baseUrl(format("http://127.0.0.1:%s/events/%d", port, positiveNumber))
                                   .build();
    Map response1 = webClient.get()
                             .exchange()
                             .flatMap(clientResponse -> clientResponse.bodyToMono(Map.class))
                             .block(); // or use WEbTestClient instead
    Event response2 = webClient.get()
                               .exchange()
                               .flatMap(clientResponse -> clientResponse.bodyToMono(Event.class))
                               .block();

    assertThat(response1.get("id")).isNotNull();
    assertThat(response2.getId()).isNotNull();
    assertThat(response1.get("id")).isEqualTo(response2.getId())
                                   .isEqualTo(positiveNumber);

    assertThat(response1.get("when")).isNotNull();
    assertThat(response2.getWhen()).isNotNull();
    assertThat(response1.get("when")).isNotEqualTo(response2.getWhen());
    assertThat(response2.getWhen()).isBeforeOrEqualTo(ZonedDateTime.now());

    log.info("resp 1: {}", response1);
    log.info("resp 2: {}", response2);
  }

  @Before
  public void before() {
    assertThat(port).isNotNull()
                    .isPositive();
  }
}

package com.kenstudy.user_service.client.account;

import com.kenstudy.user_service.exception.ErrorMessageResponse;
import com.kenstudy.user_service.exception.ResourceNotFoundException;
import com.kenstudy.user_service.exception.UserNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GenericClientService {
    private final WebClient webClient;

    public GenericClientService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build(); // or inject base URL
    }

    public <T, R> R post(String uri, T requestBody, Class<R> responseType, Map<String, ?> uriVariables) {
        return webClient.post()
                .uri(uri,uriVariables)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                    clientResponse.bodyToMono(ErrorMessageResponse.class)
                        .flatMap(body -> Mono.error(
                            new UserNotFoundException("client error: " + body.getMessage()))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                    clientResponse.bodyToMono(ErrorMessageResponse.class)
                        .flatMap(body -> Mono.error(new ResourceNotFoundException(
                            "server error: " + body.getMessage()))))
                .bodyToMono(responseType)
                .block();
    }
    public <T, R> R post(String uri, T requestBody, Class<R> responseType) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                    clientResponse.bodyToMono(ErrorMessageResponse.class)
                        .flatMap(body -> Mono.error(
                            new UserNotFoundException("client error: " + body.getMessage()))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                    clientResponse.bodyToMono(ErrorMessageResponse.class)
                        .flatMap(body -> Mono.error(
                            new ResourceNotFoundException("server error: " + body.getMessage()))))
                .bodyToMono(responseType)
                .block();
    }

    public <R> R post(String uri, Class<R> responseType, Map<String, ?> uriVariables) {
        return webClient.post()
                .uri(uri,uriVariables)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(ErrorMessageResponse.class)
                                .flatMap(body -> Mono.error(
                                        new UserNotFoundException("client error: " + body.getMessage()))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(ErrorMessageResponse.class)
                                .flatMap(body -> Mono.error(
                                        new ResourceNotFoundException("server error: " + body.getMessage()))))
                .bodyToMono(responseType)
                .block();
    }


    // Generic GET method
    public <R> R get(String uri, Class<R> responseType, Map<String, ?> uriVariables) {
        return webClient.get()
                .uri(uri,uriVariables)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(ErrorMessageResponse.class)
                                .flatMap(body -> Mono.error(
                                        new UserNotFoundException("client error: " + body.getMessage()))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(ErrorMessageResponse.class)
                                .flatMap(body -> Mono.error(
                                        new ResourceNotFoundException("server error: " + body.getMessage()))))
                .bodyToMono(responseType)
                .block();
    }
}

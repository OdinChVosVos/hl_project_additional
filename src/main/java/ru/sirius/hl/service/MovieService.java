package ru.sirius.hl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.sirius.hl.dto.MovieDto;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {

    private final Client client;
    private final ObservabilityService observabilityService;

    public void clearAll() {
        long startTime = observabilityService.startTiming();
        try {
            executeRequest(
                    client.getMovieUrl() + "/clear",
                    HttpMethod.DELETE,
                    null,
                    Void.class,
                    "Successfully cleared all movies",
                    "Failed to clear movies"
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public List<MovieDto> getAllMovies() {
        long startTime = observabilityService.startTiming();
        try {
            MovieDto[] movies = executeRequest(
                    client.getMovieUrl(),
                    HttpMethod.GET,
                    null,
                    MovieDto[].class,
                    "Successfully retrieved movies",
                    "Failed to get movies"
            );
            return Arrays.asList(movies);
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public MovieDto getMovieById(Long id) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getMovieUrl() + "/" + id,
                    HttpMethod.GET,
                    null,
                    MovieDto.class,
                    "Successfully retrieved movie with ID " + id,
                    "Movie with ID " + id + " not found",
                    true
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public void deleteMovie(Long id) {
        long startTime = observabilityService.startTiming();
        try {
            executeRequest(
                    client.getMovieUrl() + "/" + id,
                    HttpMethod.DELETE,
                    null,
                    Void.class,
                    "Successfully deleted movie with ID " + id,
                    "Movie with ID " + id + " not found",
                    true
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public MovieDto saveMovie(MovieDto movieDto) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getMovieUrl(),
                    HttpMethod.POST,
                    movieDto,
                    MovieDto.class,
                    "Successfully saved movie",
                    "Failed to save movie due to invalid or duplicate data"
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public MovieDto updateMovie(Long id, MovieDto updatedMovieDto) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getMovieUrl() + "/" + id,
                    HttpMethod.PUT,
                    updatedMovieDto,
                    MovieDto.class,
                    "Successfully updated movie with ID " + id,
                    "Movie with ID " + id + " not found",
                    true
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    private <T> T executeRequest(String url, HttpMethod method, Object body,
                                 Class<T> responseType, String successMsg,
                                 String errorMsg) {
        return executeRequest(url, method, body, responseType, successMsg, errorMsg, false);
    }

    private <T> T executeRequest(String url, HttpMethod method, Object body,
                                 Class<T> responseType, String successMsg,
                                 String errorMsg, boolean notFoundIsExpected) {
        try {
            log.info("Sending {} request to {}", method, url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "*/*");
            if (body != null) {
                headers.set("Content-Type", "application/json");
            }

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<T> response = client.rest().exchange(
                    url,
                    method,
                    entity,
                    responseType
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info(successMsg);
                return response.getBody();
            }

            log.error("{} HTTP Status: {}", errorMsg, response.getStatusCode());
            throw new RuntimeException(errorMsg);

        } catch (HttpClientErrorException e) {
            handleClientError(e, errorMsg, notFoundIsExpected);
            throw new RuntimeException(errorMsg, e);
        } catch (HttpServerErrorException e) {
            log.error("Server error: {} - Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Server error: " + errorMsg, e);
        } catch (Exception e) {
            log.error("Error communicating with external service: {}", e.getMessage());
            throw new RuntimeException("Communication error: " + errorMsg, e);
        }
    }

    private void handleClientError(HttpClientErrorException e, String errorMsg, boolean notFoundIsExpected) {
        if (notFoundIsExpected && e.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.error("Not found: {}", e.getResponseBodyAsString());
            throw new NoSuchElementException(errorMsg);
        }
        log.error("Client error: {} - Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        if (e.getStatusCode().value() == 400) {
            throw new IllegalArgumentException("Client error: " + errorMsg + ": " + e.getResponseBodyAsString(), e);
        }
        throw new RuntimeException("Client error: " + errorMsg, e);
    }
}
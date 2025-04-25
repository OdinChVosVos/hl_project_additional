package ru.hpclab.hl.module1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.hpclab.hl.module1.dto.MovieDto;
import ru.hpclab.hl.module1.dto.TicketDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {
    private final MovieService movieService;
    private final Client client;
    private final CacheService<MovieDto> movieCache;


    public void clearAll() {
        executeRequest(
                client.getTicketUrl() + "/clear",
                HttpMethod.DELETE,
                null,
                Void.class,
                "Successfully cleared all tickets",
                "Failed to clear tickets"
        );
    }

    public List<TicketDto> getAllTickets() {
        TicketDto[] tickets = executeRequest(
                client.getTicketUrl(),
                HttpMethod.GET,
                null,
                TicketDto[].class,
                "Successfully retrieved tickets",
                "Failed to get tickets"
        );
        return Arrays.asList(tickets);
    }

    public TicketDto getTicketById(Long id) {
        return executeRequest(
                client.getTicketUrl() + "/" + id,
                HttpMethod.GET,
                null,
                TicketDto.class,
                "Successfully retrieved ticket with ID " + id,
                "Ticket with ID " + id + " not found",
                true
        );
    }

    public void deleteTicket(Long id) {
        executeRequest(
                client.getTicketUrl() + "/" + id,
                HttpMethod.DELETE,
                null,
                Void.class,
                "Successfully deleted ticket with ID " + id,
                "Ticket with ID " + id + " not found",
                true
        );
    }

    public TicketDto saveTicket(TicketDto ticketDto) {
        return executeRequest(
                client.getTicketUrl(),
                HttpMethod.POST,
                ticketDto,
                TicketDto.class,
                "Successfully saved ticket",
                "Failed to save ticket due to invalid or duplicate data"
        );
    }

    public TicketDto updateTicket(Long id, TicketDto updatedTicketDto) {
        return executeRequest(
                client.getTicketUrl() + "/" + id,
                HttpMethod.PUT,
                updatedTicketDto,
                TicketDto.class,
                "Successfully updated ticket with ID " + id,
                "Ticket with ID " + id + " not found",
                true
        );
    }

    public Map<LocalDate, Long> getMaxViewersByDay(String movieName) {
        try {
            log.info("Calculating max viewers by day for movie: {}", movieName);

            List<TicketDto> filteredTickets = getAllTickets().stream()
                    .filter(ticket -> {
                        try {
                            MovieDto cachedMovie = movieCache.get(ticket.getMovie());

                            if (cachedMovie != null) {
                                return movieName.equalsIgnoreCase(cachedMovie.getName());
                            }

                            MovieDto movie = movieService.getMovieById(ticket.getMovie());

                            // Cache the movie for future lookups
                            if (movie != null) {
                                movieCache.set(ticket.getMovie(), movie);
                                return movieName.equalsIgnoreCase(movie.getName());
                            }
                            return false;

                        } catch (Exception e) {
                            log.warn("Failed to fetch movie with ID {}: {}", ticket.getMovie(), e.getMessage());
                            return false;
                        }
                    })
                    .toList();

            log.info("Found {} tickets for movie '{}'", filteredTickets.size(), movieName);

            if (filteredTickets.isEmpty()) {
                log.warn("No tickets found for movie: {}", movieName);
                return Collections.emptyMap();
            }

            Map<LocalDateTime, Long> sessionViewers = filteredTickets.stream()
                    .collect(Collectors.groupingBy(
                            TicketDto::getSessionDate,
                            Collectors.counting()
                    ));

            Map<LocalDate, Long> dailyPeaks = sessionViewers.entrySet().stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().toLocalDate(),
                            Collectors.collectingAndThen(
                                    Collectors.mapping(
                                            Map.Entry::getValue,
                                            Collectors.maxBy(Long::compare)
                                    ),
                                    opt -> opt.orElse(0L)
                            )));

            log.info("Daily peak viewers for '{}': {}", movieName, dailyPeaks);
            return dailyPeaks;

        } catch (Exception e) {
            log.error("Failed to calculate peak viewers for '{}': {}", movieName, e.getMessage(), e);
            throw new RuntimeException("Peak viewer calculation failed", e);
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
            throw new RuntimeException(errorMsg, e); // This line won't be reached
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
    }
}
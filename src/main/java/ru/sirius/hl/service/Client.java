package ru.sirius.hl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Client {
    private final String appHost;
    private final RestTemplate restTemplate;

    public Client(@Value("${app.host:app}") String appHost, RestTemplate restTemplate) {
        this.appHost = appHost;
        this.restTemplate = restTemplate;
    }

    public String getTicketUrl() {
        return String.format("http://%s:8080/api/v1/ticket", appHost);
    }

    public String getMovieUrl() {
        return String.format("http://%s:8080/api/v1/movie", appHost);
    }

    public String getCustomerUrl() {
        return String.format("http://%s:8080/api/v1/customer", appHost);
    }

    public RestTemplate rest() {
        return restTemplate;
    }
}
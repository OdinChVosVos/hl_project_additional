package ru.sirius.hl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.sirius.hl.dto.CustomerDto;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final Client client;
    private final ObservabilityService observabilityService;

    public void clearAll() {
        long startTime = observabilityService.startTiming();
        try {
            executeRequest(
                    client.getCustomerUrl() + "/clear",
                    HttpMethod.DELETE,
                    null,
                    Void.class,
                    "Successfully cleared all customers",
                    "Failed to clear customers"
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public List<CustomerDto> getAllCustomers() {
        long startTime = observabilityService.startTiming();
        try {
            CustomerDto[] customers = executeRequest(
                    client.getCustomerUrl(),
                    HttpMethod.GET,
                    null,
                    CustomerDto[].class,
                    "Successfully retrieved customers",
                    "Failed to get customers"
            );
            return Arrays.asList(customers);
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public CustomerDto getCustomerById(Long id) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getCustomerUrl() + "/" + id,
                    HttpMethod.GET,
                    null,
                    CustomerDto.class,
                    "Successfully retrieved customer with ID " + id,
                    "Customer with ID " + id + " not found",
                    true
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public void deleteCustomer(Long id) {
        long startTime = observabilityService.startTiming();
        try {
            executeRequest(
                    client.getCustomerUrl() + "/" + id,
                    HttpMethod.DELETE,
                    null,
                    Void.class,
                    "Successfully deleted customer with ID " + id,
                    "Customer with ID " + id + " not found",
                    true
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public CustomerDto saveCustomer(CustomerDto customerDto) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getCustomerUrl(),
                    HttpMethod.POST,
                    customerDto,
                    CustomerDto.class,
                    "Successfully saved customer",
                    "Failed to save customer due to invalid data"
            );
        } finally {
            observabilityService.stopTiming(startTime, "external");
        }
    }

    public CustomerDto updateCustomer(Long id, CustomerDto updatedCustomerDto) {
        long startTime = observabilityService.startTiming();
        try {
            return executeRequest(
                    client.getCustomerUrl() + "/" + id,
                    HttpMethod.PUT,
                    updatedCustomerDto,
                    CustomerDto.class,
                    "Successfully updated customer with ID " + id,
                    "Customer with ID " + id + " not found",
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
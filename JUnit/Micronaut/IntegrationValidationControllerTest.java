package com.ssi.integration;

import com.ssi.*;
import io.micronaut.http.*;
import io.micronaut.http.client.*;
import io.micronaut.http.client.annotation.*;
import io.micronaut.http.client.exceptions.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class IntegrationValidationControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void integrationIsValidated() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> httpClient.toBlocking().exchange(HttpRequest.POST("/ssi", new IntegrationCommand("", null, null, null, null, null, null)))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
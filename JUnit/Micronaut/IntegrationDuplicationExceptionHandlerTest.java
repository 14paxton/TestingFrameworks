//package com.ssi.integration;
//
//import io.micronaut.http.*;
//import io.micronaut.http.client.*;
//import io.micronaut.http.client.annotation.*;
//import io.micronaut.http.client.exceptions.*;
//import io.micronaut.test.extensions.junit5.annotation.*;
//import jakarta.inject.*;
//import org.junit.jupiter.api.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@MicronautTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class IntegrationDuplicationExceptionHandlerTest extends BaseTest {
//    @Inject
//    @Client("/")
//    HttpClient httpClient;
//
//    @Test
//    void duplicatedIntegrationsReturns400() {
//        IntegrationCommand banana = new IntegrationCommand("Banana");
//        HttpRequest<?> request = HttpRequest.POST("/ssi", banana);
//        HttpResponse<?> response = httpClient.toBlocking().exchange(request);
//        assertEquals(HttpStatus.CREATED, response.status());
//        HttpClientResponseException exception = assertThrows(
//                HttpClientResponseException.class,
//                () -> httpClient.toBlocking().exchange(request)
//        );
//        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
//        HttpRequest<?> deleteRequest = HttpRequest.DELETE("/ssi", banana);
//        HttpResponse<?> deleteResponse = httpClient.toBlocking().exchange(deleteRequest);
//        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.status());
//    }
//}
package com.ssi.integration

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class IntegrationValidationControllerSpec extends Specification{

    @Inject
    @Client("/")
    HttpClient httpClient;

    void "integrationIsValidated"() {
        when:
        httpClient.toBlocking().exchange(HttpRequest.POST("/ssi", new IntegrationCommand(null, "", null, null, null, null, null, null, null)))

        then:
        def e = thrown(HttpClientResponseException.class)
        e.status == HttpStatus.BAD_REQUEST
    }
}
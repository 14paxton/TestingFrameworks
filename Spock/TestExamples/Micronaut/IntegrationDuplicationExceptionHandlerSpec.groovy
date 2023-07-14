package com.ssi.integration

import com.ssi.Enums.AssessmentReportUrlType
import com.ssi.request.WSConfigCommand
import com.ssi.result.ClientResultCommand;//package com.ssi.integration;

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class IntegrationDuplicationExceptionHandlerSpec extends BaseTestSpec {
    @Inject
    @Client("/")
    HttpClient httpClient;

    void "duplicatedIntegrationsReturns400"() {
        when:
        WSConfigCommand wsCMD = new WSConfigCommand(null, null, null, null, null, null, null, null, null, null, null, null, null, null )
        ClientResultCommand resultCMD = new ClientResultCommand(null, null, null, null, AssessmentReportUrlType.REQ_CORE_AUTH, null, true, null, true, null, null, null)
        IntegrationCommand intCMD = new IntegrationCommand(null, "intCMD", null, null, null, null, wsCMD, resultCMD, null);
        HttpRequest<?> request = HttpRequest.POST("/ssi", intCMD);
        HttpResponse<?> response = httpClient.toBlocking().exchange(request);

        then:
        HttpStatus.CREATED == response.status()

        when:
        httpClient.toBlocking().exchange(request)

        then:
        HttpClientResponseException e = thrown()
        HttpStatus.UNPROCESSABLE_ENTITY == e.status

        when:
        HttpRequest<?> deleteRequest = HttpRequest.DELETE("/ssi", intCMD)
        HttpResponse<?> deleteResponse = httpClient.toBlocking().exchange(deleteRequest)

        then:
        HttpStatus.NO_CONTENT == deleteResponse.status()
    }
}
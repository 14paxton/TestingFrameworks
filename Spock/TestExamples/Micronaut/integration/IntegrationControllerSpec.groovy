package com.ssi.integration

import com.ssi.Enums.AssessmentReportUrlType
import com.ssi.request.WSConfigCommand
import com.ssi.result.ClientResultCommand
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared

import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

@MicronautTest(environments = ["test"], packages = "com.ssi.*", propertySources = "classpath:application-test.yml")
class IntegrationControllerSpec extends BaseTestSpec {
    @Shared
    WSConfigCommand wsCMD = new WSConfigCommand(null, null, null, null, null, null, null, null, null, null, null, null, null, "test me")
    @Shared
    ClientResultCommand resultCMD = new ClientResultCommand(null, null, null, null, AssessmentReportUrlType.REQ_CORE_AUTH, null, null, true, null, true, null, null, "test me result")
    @Shared
    Map<String, LinkedList<ClientEntityDetails>> clientEntityMap = ["GNDR_MAP": new LinkedList<ClientEntityDetails>([new ClientEntityDetails("gender stuff", "crankin", "DECLINE")]),
            "ETHNIC_MAP": new LinkedList<ClientEntityDetails>([new ClientEntityDetails("phone stuff", "crankin", "OTHER")])
    ] as Map<String, LinkedList<ClientEntityDetails>>

    void "test controller integration actions"() {
        given:
        IntegrationCommand intCMD = new IntegrationCommand("intCMD", null, null, null, "strong_password", wsCMD, resultCMD, clientEntityMap);
        String company2Name = "company2Name";
        String company2Password = "Yellow_and_curved";
        Map<String, Object> properties = new HashMap<>(getProperties());
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, properties)
        IntegrationClient integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient)

        when:
        IntegrationCommand newIntCMD = new IntegrationCommand( company2Name, null, null, "company_2", null, wsCMD, resultCMD, clientEntityMap)
        HttpResponse<Integration> response = integrationClient.create(newIntCMD);

        then:
        HttpStatus.CREATED == response.getStatus()
        response.getBody().isPresent()

        when:
        Integration company2 = response.getBody().get();

        List<Integration> integrationList = integrationList(integrationClient);

        then:
        1 == integrationList.size()
        company2.getId() == integrationList.get(0).getId()
        integrationList.get(0).getTbeCompanyCode()

        when:
        Optional<Integration> company2Optional = integrationClient.update(intCMD);

        then:
        !company2Optional.isPresent()

        when:
        response = integrationClient.create(intCMD);

        then:
        HttpStatus.CREATED == response.getStatus()
        integrationStream(integrationClient)
                .anyMatch(f -> "company2Name" == f.getCompanyCode())

        when:
        company2Optional = integrationClient.update(new IntegrationCommand( company2Name, null, null, null, company2Password, wsCMD, resultCMD, clientEntityMap));
        then:
        company2Optional.isPresent()
        Stream.of("Yellow_and_curved", "strong_password").collect(Collectors.toSet()) ==
        integrationStream(integrationClient)
                .map(Integration::getTbexPassword)
                .collect(Collectors.toSet())

        when:
        embeddedServer.close();
        embeddedServer = ApplicationContext.run(EmbeddedServer, properties)
        integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient)

        then:
        2 == numberOfIntegrations(integrationClient)

        when:
        integrationClient.delete(intCMD);
        integrationClient.delete(new IntegrationCommand(company2Name, company2Password, null, null, null,  wsCMD, resultCMD, clientEntityMap));
        embeddedServer.close()
        embeddedServer = ApplicationContext.run(EmbeddedServer, properties)
        integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient)


        then:
        0 == numberOfIntegrations(integrationClient)

        cleanup:
        embeddedServer.close()
    }

    private int numberOfIntegrations(IntegrationClient integrationClient) {
        return integrationList(integrationClient).size();
    }

    private List<Integration> integrationList(IntegrationClient integrationClient) {
        return integrationStream(integrationClient)
                .collect(Collectors.toList());
    }

    private Stream<Integration> integrationStream(IntegrationClient integrationClient) {
        Iterable<Integration> integrations = integrationClient.list();
        return StreamSupport.stream(integrations.spliterator(), false);
    }

}
//package com.ssi.integration;
//
//import com.ssi.*;
//import io.micronaut.context.ApplicationContext;
//import io.micronaut.http.HttpResponse;
//import io.micronaut.http.HttpStatus;
//import io.micronaut.runtime.server.EmbeddedServer;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//import java.util.stream.StreamSupport;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class IntegrationControllerTest extends BaseTest {
//
//    @Test
//    void testInteractionWithTheController() {
//        IntegrationCommand intCMD = new IntegrationCommand("intCMD", "strong_pasword", null, null, null);
//        String company2Name = "company2Name";
//        String company2Password = "Yellow_and_curved";
//        Map<String, Object> properties = new HashMap<>(getProperties());
//        try (EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer.class, properties)) {
//            IntegrationClient integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient.class);
//            HttpResponse<Integration> response = integrationClient.create(new IntegrationCommand(company2Name));
//            assertEquals(HttpStatus.CREATED, response.getStatus());
//            assertTrue(response.getBody().isPresent());
//            Integration company2 = response.getBody().get();
//
//            List<Integration> integrationList = integrationList(integrationClient);
//            assertEquals(1, integrationList.size());
//            assertEquals(company2.getId(), integrationList.get(0).getId());
//            assertNull(integrationList.get(0).getTbeCompanyCode());
//
//            Optional<Integration> company2Optional = integrationClient.update(intCMD);
//            assertFalse(company2Optional.isPresent());
//
//            response = integrationClient.create(intCMD);
//            assertEquals(HttpStatus.CREATED, response.getStatus());
//
//            assertTrue(integrationStream(integrationClient)
//                    .anyMatch(f -> "company2Name".equals(f.getCompanyCode())));
//            company2Optional = integrationClient.update(new IntegrationCommand(company2Name, company2Password, null, null, null));
//            assertTrue(company2Optional.isPresent());
//            assertEquals(
//                    Stream.of("Yellow_and_curved", "strong_pasword").collect(Collectors.toSet()),
//                    integrationStream(integrationClient)
//                            .map(Integration::getTbexPassword)
//                            .collect(Collectors.toSet())
//            );
//        }
//        try (EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer.class, properties)) {
//            IntegrationClient integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient.class);
//            assertEquals(2, numberOfIntegrations(integrationClient));
//            integrationClient.delete(intCMD);
//            integrationClient.delete(new IntegrationCommand(company2Name, company2Password, null, null, null));
//        }
//        try (EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer.class, properties)) {
//            IntegrationClient integrationClient = embeddedServer.getApplicationContext().getBean(IntegrationClient.class);
//            assertEquals(0, numberOfIntegrations(integrationClient));
//        }
//    }
//
//    private int numberOfIntegrations(IntegrationClient integrationClient) {
//        return integrationList(integrationClient).size();
//    }
//
//    private List<Integration> integrationList(IntegrationClient integrationClient) {
//        return integrationStream(integrationClient)
//                .collect(Collectors.toList());
//    }
//
//    private Stream<Integration> integrationStream(IntegrationClient integrationClient) {
//        Iterable<Integration> integrations = integrationClient.list();
//        return StreamSupport.stream(integrations.spliterator(), false);
//    }
//}
package com.ssi.integration


import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import javax.validation.ConstraintViolationException;


@MicronautTest(startApplication = false)
class IntegrationRepositorySpec extends Specification {

    @Inject
    IntegrationRepo integrationRepository;

    void "methodsValidateParamers"() {
        when:
        integrationRepository.create(new IntegrationCommand(null,"", null, null, null, null, null, null, null));
        then:
        thrown(ConstraintViolationException)
    }
}
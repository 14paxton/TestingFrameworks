package com.ssi.integration;

import com.ssi.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;

import javax.validation.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class IntegrationRepositoryTest {

    @Inject
    IntegrationRepo integrationRepository;

    @Test
    void methodsValidateParamers() {
        Executable e = () -> integrationRepository.create(new IntegrationCommand("", null, null, null, null, null, null));
        assertThrows(ConstraintViolationException.class, e);
    }
}
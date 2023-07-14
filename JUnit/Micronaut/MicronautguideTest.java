package com.ssi.integration;

import io.micronaut.runtime.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;
import org.junit.jupiter.api.*;

@MicronautTest
class MicronautguideTest {

    @Inject
    EmbeddedApplication<?> application;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

}
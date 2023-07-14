package com.ssi.integration

import io.micronaut.core.annotation.NonNull
import io.micronaut.test.support.TestPropertyProvider
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

abstract class BaseTestSpec extends Specification implements TestPropertyProvider {

    @Override
    @NonNull
    Map<String, String> getProperties() {
        Path tempDir = Files.createTempDirectory('microstream')
        ["microstream.storage.main.storage-directory": tempDir.toString()]
    }
}
package com.ssi.integration;

import io.micronaut.core.annotation.*;
import io.micronaut.test.support.*;
import org.junit.jupiter.api.io.*;

import java.io.*;
import java.util.*;

 abstract class BaseTest implements TestPropertyProvider {

    @TempDir
    static File tempDir;

    @Override
    @NonNull
    public Map<String, String> getProperties() {
        return Collections.singletonMap(
                "microstream.storage.main.storage-directory", tempDir.getAbsolutePath()
        );
    }
}
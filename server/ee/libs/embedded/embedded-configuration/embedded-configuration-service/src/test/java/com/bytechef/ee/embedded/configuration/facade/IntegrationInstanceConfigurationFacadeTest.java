/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceConfigurationFacadeTest {

    @Test
    void testTheListReadStillHasExactlyOneCaller() throws IOException {
        Path sourceRoot = moduleSourceRoot();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> callers = paths.filter(path -> path.toString()
                .endsWith(".java"))
                .filter(path -> {
                    try {
                        String source = Files.readString(path);

                        return source.contains("getIntegrationInstanceConfigurationIntegrations(")
                            && !path.endsWith("IntegrationInstanceConfigurationFacadeImpl.java");
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .map(path -> String.valueOf(path.getFileName()))
                .toList();

            assertThat(callers)
                .as(
                    "a new caller of the unguarded list read must apply its own per-row filtering; see the method's " +
                        "Javadoc")
                .containsExactly("ConnectedUserIntegrationFacadeImpl.java");
        }
    }

    private static Path moduleSourceRoot() {
        Path path;

        try {
            path = Path.of(
                IntegrationInstanceConfigurationFacadeTest.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }

        while (path != null && !Files.isDirectory(path.resolve("src/main/java"))) {
            path = path.getParent();
        }

        if (path == null) {
            throw new IllegalStateException("Could not locate the module's src/main/java");
        }

        return path.resolve("src/main/java");
    }

    @Test
    void testTheListReadStillHasExactlyOneCallerWorthOfArguments() throws NoSuchMethodException {
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegrations", boolean.class, Environment.class);

        assertThat(Arrays.stream(method.getParameterTypes()))
            .noneMatch(parameterType -> parameterType == long.class || parameterType == Long.class);
    }
}

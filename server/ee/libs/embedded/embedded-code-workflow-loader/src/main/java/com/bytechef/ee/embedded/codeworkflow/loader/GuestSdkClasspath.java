/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the SDK jars bundled under {@code META-INF/guest-sdk/embedded/} to a temporary directory so they can be put
 * on the classpath of the GraalVM Espresso guest JVM. Uploaded code workflow jars are thin: they compile against the
 * integration-api and workflow-api SDKs, which the guest JVM cannot resolve from the host.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class GuestSdkClasspath {

    private static final String RESOURCE_PREFIX = "META-INF/guest-sdk/embedded/";

    private static volatile String classpath;

    private GuestSdkClasspath() {
    }

    static String get() {
        if (classpath == null) {
            synchronized (GuestSdkClasspath.class) {
                if (classpath == null) {
                    classpath = extract();
                }
            }
        }

        return classpath;
    }

    private static String extract() {
        ClassLoader classLoader = GuestSdkClasspath.class.getClassLoader();

        try {
            Path tempDirectory = Files.createTempDirectory("bytechef_guest_sdk_embedded");

            List<String> jarPaths = new ArrayList<>();

            for (String jarName : readIndex(classLoader)) {
                try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + jarName)) {
                    if (inputStream == null) {
                        throw new IllegalStateException(
                            "Guest SDK jar resource %s%s is missing".formatted(RESOURCE_PREFIX, jarName));
                    }

                    Path jarPath = tempDirectory.resolve(jarName);

                    Files.copy(inputStream, jarPath);

                    jarPaths.add(jarPath.toString());
                }
            }

            return String.join(File.pathSeparator, jarPaths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readIndex(ClassLoader classLoader) throws IOException {
        try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + "index.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException(
                    "Guest SDK index resource %sindex.txt is missing".formatted(RESOURCE_PREFIX));
            }

            String index = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return index.lines()
                .filter(line -> !line.isBlank())
                .toList();
        }
    }
}

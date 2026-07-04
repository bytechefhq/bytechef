/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class GuestSdkClasspathTest {

    @Test
    void testGetReturnsExtractedSdkJars() {
        String classpath = GuestSdkClasspath.get();

        String[] jarPaths = classpath.split(File.pathSeparator);

        assertTrue(
            jarPaths.length >= 2, "Expected at least integration-api and workflow-api jars, got: " + classpath);

        boolean integrationApiFound = false;
        boolean workflowApiFound = false;

        for (String jarPath : jarPaths) {
            assertTrue(Files.isRegularFile(Paths.get(jarPath)), "Extracted jar does not exist: " + jarPath);

            if (jarPath.contains("integration-api")) {
                integrationApiFound = true;
            }

            if (jarPath.contains("workflow-api")) {
                workflowApiFound = true;
            }
        }

        assertTrue(integrationApiFound, "integration-api jar missing from: " + classpath);
        assertTrue(workflowApiFound, "workflow-api jar missing from: " + classpath);
    }
}

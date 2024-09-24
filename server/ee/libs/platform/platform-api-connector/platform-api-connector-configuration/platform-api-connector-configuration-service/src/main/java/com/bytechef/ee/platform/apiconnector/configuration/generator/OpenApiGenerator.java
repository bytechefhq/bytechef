/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.generator;

import com.bytechef.cli.command.component.init.openapi.ComponentInitOpenApiGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenApiGenerator {

    private static final String[] LIBS = new String[] {
        "/libs/auto-service-annotations-1.1.1.jar",
        "/libs/component-api-1.0.jar",
        "/libs/definition-api-1.0.jar"
    };

    public static Path generate(String componentName, Path openApiSpecificationPath) throws Exception {
        Path jarsDir = Files.createTempDirectory("jars");
        Path outputPath = Files.createTempDirectory("open_api_output");

        for (String lib : LIBS) {
            extractJarFromResource(lib, jarsDir);
        }

        try {
            ComponentInitOpenApiGenerator generator = new ComponentInitOpenApiGenerator(
                "com.bytechef.component", componentName, 1, true, openApiSpecificationPath.toString(),
                outputPath.toString(), jarsDir.toString());

            generator.generate();

            return outputPath.resolve(componentName)
                .resolve("src/test/resources/definition")
                .resolve(componentName + "_v1.json");
        } finally {
            deleteDirectoryRecursively(jarsDir.toFile());
        }
    }

    private static void extractJarFromResource(String resourcePath, Path destDir) throws Exception {
        Path jarPath = destDir.resolve(new File(resourcePath).getName());

        try (InputStream is = OpenApiGenerator.class.getResourceAsStream(resourcePath)) {
            Files.copy(is, jarPath);
        }
    }

    @SuppressFBWarnings("RV")
    private static void deleteDirectoryRecursively(File dir) {
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                deleteDirectoryRecursively(file);
            }
        }

        dir.delete();
    }
}

/*
 * Copyright 2025 ByteChef
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

package com.bytechef.cli.command.component.init;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ComponentInitCommandTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        Path outputPath = tempDir.resolve("petstore");

        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void testComponentInitGeneratesExpectedFiles() {
        URL url = ComponentInitCommandTest.class.getResource("/dependencies/petstore.yaml");
        String outputPath = tempDir.toAbsolutePath()
            .toString();

        CliApplication.main(
            "component", "init", "--open-api-path", url.getFile(), "--output-path", outputPath, "--name", "petstore");

        Path componentDir = tempDir.resolve("petstore");

        assertTrue(Files.exists(componentDir), "Component directory should be created");

        Path mainJavaDir = componentDir.resolve("src/main/java/com/bytechef/component/petstore");

        assertTrue(
            Files.exists(mainJavaDir.resolve("PetstoreComponentHandler.java")),
            "ComponentHandler should be generated");
        assertTrue(
            Files.exists(mainJavaDir.resolve("AbstractPetstoreComponentHandler.java")),
            "AbstractComponentHandler should be generated");
        assertTrue(
            Files.exists(mainJavaDir.resolve("connection/PetstoreConnection.java")),
            "Connection class should be generated");

        Path actionDir = mainJavaDir.resolve("action");

        assertTrue(Files.exists(actionDir), "Action directory should be created");
        assertTrue(
            Files.exists(actionDir.resolve("PetstoreListPetsAction.java")),
            "ListPets action should be generated");
    }

    @Test
    void testComponentInitWithDifferentOpenApiSpec() {
        URL url = ComponentInitCommandTest.class.getResource("/dependencies/petstore2.yaml");
        String outputPath = tempDir.toAbsolutePath()
            .toString();

        CliApplication.main(
            "component", "init", "--open-api-path", url.getFile(), "--output-path", outputPath, "--name", "petstore");

        Path componentDir = tempDir.resolve("petstore");

        assertTrue(Files.exists(componentDir), "Component directory should be created");

        Path mainJavaDir = componentDir.resolve("src/main/java/com/bytechef/component/petstore");

        assertTrue(
            Files.exists(mainJavaDir.resolve("PetstoreComponentHandler.java")),
            "ComponentHandler should be generated");
    }
}

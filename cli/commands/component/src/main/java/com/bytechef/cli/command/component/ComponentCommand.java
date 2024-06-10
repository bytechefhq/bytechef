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

package com.bytechef.cli.command.component;

import com.bytechef.cli.command.component.init.openapi.ComponentInitOpenApiGenerator;
import java.io.File;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "component", description = "Manage, create, and publish a component")
public class ComponentCommand {

    @Command
    public void component() {
    }

    @Command(command = "init", description = "Generates project for a new component.")
    public void init(
        @Option(
            longNames = "base-package-name", label = "NAME", description = "package for generated classes",
            defaultValue = "com.bytechef.component") String basePackageName,
        @Option(
            longNames = "internal-component",
            description = "if a component is the internal one(ships with the platform) or the custom one",
            defaultValue = "false") boolean internalComponent,
        @Option(
            longNames = "name", shortNames = 'n', label = "NAME", description = "component name",
            required = true) String name,
        @Option(
            longNames = "open-api-path", label = "PATH",
            description = "path to the OpenAPI specification") String openApiPath,
        @Option(
            longNames = "output-path", shortNames = 'o', label = "PATH",
            description = "where to write the generated files (current dir by default)",
            required = true) String outputPath,
        @Option(
            longNames = "version", shortNames = 'v', label = "VERSION", description = "the component version",
            defaultValue = "1") int version)
        throws Exception {

        if (openApiPath != null && !openApiPath.isEmpty()) {
            generateOpenApiComponent(basePackageName, internalComponent, name, openApiPath, outputPath, version);
        }
    }

    private void generateOpenApiComponent(
        String basePackageName, boolean internalComponent, String name, String openApiPath, String outputPath,
        int version) throws Exception {

        if (!openApiPath.matches("^http(s)?://.*") && !new File(openApiPath).exists()) {
            throw new RuntimeException("The OpenAPI file is not found: " + openApiPath);
        }

        ComponentInitOpenApiGenerator generator = new ComponentInitOpenApiGenerator(
            basePackageName, name.toLowerCase(), version, internalComponent, openApiPath, outputPath);

        generator.generate();
    }
}

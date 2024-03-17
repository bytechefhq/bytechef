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

package com.bytechef.cli.command.component.init;

import com.bytechef.cli.command.component.ComponentCommand;
import com.bytechef.cli.command.component.init.openapi.ComponentInitOpenApiGenerator;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * @author Ivica Cardic
 */
@CommandLine.Command(name = "init", description = "Generates project for a new component.")
public class ComponentInitCommand implements Callable<Integer> {

    @CommandLine.Spec
    private transient CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Option(
        names = {
            "--base-package-name"
        },
        paramLabel = "base package name",
        description = "package for generated classes",
        defaultValue = "com.bytechef.component")
    private transient String basePackageName;

    @CommandLine.Option(
        names = {
            "--open-api-path"
        },
        description = "path to the OpenAPI specification")
    private transient String openApiPath;

    @CommandLine.Option(
        names = {
            "-o", "--output-path"
        },
        paramLabel = "output directory",
        description = "where to write the generated files (current dir by default)",
        defaultValue = "")
    private transient String outputPath;

    @CommandLine.ParentCommand
    private ComponentCommand componentCommand;

    @CommandLine.Parameters(paramLabel = "COMPONENT", description = "component name")
    private transient String componentName;

    @CommandLine.Option(
        hidden = true,
        names = {
            "--internal-component"
        },
        paramLabel = "internal component",
        description = "if a component is the internal one(ships with the platform) or the custom one",
        defaultValue = "false")
    private transient boolean internalComponent;

    @CommandLine.Option(
        names = {
            "--version"
        },
        paramLabel = "component version",
        description = "the component version",
        defaultValue = "1")
    private transient int version = 1;

    @Override
    public Integer call() throws Exception {
        if (openApiPath != null && !openApiPath.isEmpty()) {
            generateOpenApiComponent();
        }

        return null;
    }

    private void generateOpenApiComponent() throws Exception {
        if (!openApiPath.matches("^http(s)?://.*") && !new File(openApiPath).exists()) {
            throw new CommandLine.ParameterException(
                commandSpec.commandLine(), "The OpenAPI file is not found: " + openApiPath);
        }

        new ComponentInitOpenApiGenerator(basePackageName, componentName.toLowerCase(), version, internalComponent,
            openApiPath, outputPath)
                .generate();
    }
}

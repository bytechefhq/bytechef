/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.cli.command.component.subcommand;

import java.io.File;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

/**
 * @author Ivica Cardic
 */
@CommandLine.Command(name = "init", description = "Generates project for a new component)")
public class InitSubcommand implements Callable<Integer> {

    @CommandLine.Spec
    private transient CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Option(
            names = {"--base-package-name"},
            paramLabel = "base package name",
            description = "package for generated classes",
            defaultValue = "com.bytechef.component")
    private transient String basePackageName;

    @CommandLine.Option(
            names = {"--open-api-path"},
            description = "path to the OpenAPI specification")
    private transient String openApiPath;

    @CommandLine.Option(
            names = {"-o", "--output-path"},
            paramLabel = "output directory",
            description = "where to write the generated files (current dir by default)",
            defaultValue = "")
    private transient String outputPath;

    @CommandLine.Option(
            names = {"--standard-component"},
            paramLabel = "standard component",
            description = "if a component is the standard one(ships with the platform) or the custom one",
            defaultValue = "false")
    private transient boolean standardComponent;

    @CommandLine.Parameters(paramLabel = "COMPONENT", description = "component name")
    private transient String componentName;

    @Override
    public Integer call() throws Exception {
        if (StringUtils.isNotEmpty(openApiPath)) {
            generateOpenAPIComponent();
        }

        return null;
    }

    private void generateOpenAPIComponent() throws Exception {
        if (!openApiPath.matches("^http(s)?://.*") && !new File(openApiPath).exists()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(), "The OpenAPI file is not found: " + openApiPath);
        }

        new RestComponentGenerator(basePackageName, componentName, openApiPath, outputPath).generate();
    }
}

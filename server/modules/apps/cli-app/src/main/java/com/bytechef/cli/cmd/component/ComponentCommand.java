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

package com.bytechef.cli.cmd.component;

import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static picocli.CommandLine.*;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Model.CommandSpec;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.Generator;
import org.openapitools.codegen.api.TemplateDefinition;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ParameterException;

/**
 * @author Ivica Cardic
 */
@Command(name = "component", description = "Generates project for a new component", mixinStandardHelpOptions = true)
public class ComponentCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCommand.class);

    @Spec
    private CommandSpec commandSpec;

    @Option(
            names = {"--base-package-name"},
            paramLabel = "base package name",
            description = "package for generated classes",
            defaultValue = "com.bytechef.atlas")
    private String basePackageName;

    @Option(
            names = {"--component-version"},
            paramLabel = "component version",
            description = "component version",
            defaultValue = "1.0")
    private float componentVersion;

    @Option(
            names = {"--open-api-path"},
            description = "path to the OpenAPI specification")
    private String openAPIPath;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "output directory",
            description = "where to write the generated files (current dir by default)",
            defaultValue = ".")
    private String output;

    @Option(
            names = {"--standard-component"},
            paramLabel = "standard component",
            description = "if a component is the standard one(ships with the platform) or the custom one",
            defaultValue = "true")
    private boolean standardComponent;

    @Parameters(paramLabel = "COMPONENT", description = "component name")
    private String componentName;

    @Override
    public Integer call() throws Exception {
        if (isNotEmpty(openAPIPath)) {
            OpenAPIComponentGenerator openAPIComponentGenerator = new OpenAPIComponentGenerator();

            openAPIComponentGenerator.generate();
        }

        return 0;
    }

    private String getPackageName(String subPackage) {
        return (deleteWhitespace((basePackageName == null ? "" : basePackageName + "."))
                + replaceChars(componentName, "-_", ".")
                + ".v"
                + String.valueOf(componentVersion).replace(".", "_")
                + (subPackage == null ? "" : "." + subPackage));
    }

    private class OpenAPIComponentGenerator {

        private static final String[] EXCLUDE_TEMPLATES = {
            ".github",
            "gradle",
            ".gitignore",
            ".travis.yml",
            "src/main/AndroidManifest.xml",
            "build.sbt",
            "git_push.sh",
            "git_push.sh",
            "gradle.properties",
            "gradlew",
            "gradlew.bat",
            "pom.xml",
            "settings.gradle",
        };

        private static final List<TemplateDefinition> USER_DEFINED_TEMPLATE_DEFINITIONS = List.of(
                new TemplateDefinition("task_constants.mustache", "src/main/java", "TaskConstants.java"),
                new TemplateDefinition(
                        "task_description_handler.mustache", "src/main/java", "TaskDescriptorHandler.java"),
                new TemplateDefinition("task_handler.mustache", "src/main/java", "TaskHandler.java"));

        private Generator generator;

        private OpenAPIComponentGenerator() {
            if (!openAPIPath.matches("^http(s)?://.*") && !new File(openAPIPath).exists()) {
                throw new ParameterException(
                        commandSpec.commandLine(), "The OpenAPI file is not found: " + openAPIPath);
            }

            CodegenConfigurator configurator = new ByteChefCodegenConfigurator();

            configurator.setApiPackage(getPackageName("api"));
            configurator.setArtifactVersion(String.valueOf(componentVersion));
            configurator.setGeneratorName("java");
            configurator.setInputSpec(openAPIPath);
            configurator.setLibrary("native");
            configurator.setModelPackage(getPackageName("model"));
            configurator.setOutputDir(output);
            configurator.setPackageName(getPackageName(null));
            configurator.setTemplateDir("templates/component/rest");

            generator = new DefaultGenerator();

            generator.opts(configurator.toClientOptInput());
        }

        private void generate() {
            generator.generate();

            if (standardComponent) {
                removeGeneratedFiles();
            }
        }

        private void removeGeneratedFiles() {
            for (String file : EXCLUDE_TEMPLATES) {
                Path path = Path.of(output + File.separator + file);

                try {
                    if (path.toFile().isDirectory()) {
                        Files.walk(path)
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } else {
                        Files.delete(path);
                    }
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                }
            }
        }

        private static class ByteChefCodegenConfigurator extends CodegenConfigurator {

            @Override
            public ClientOptInput toClientOptInput() {
                ClientOptInput clientOptInput = super.toClientOptInput();

                clientOptInput.userDefinedTemplates(USER_DEFINED_TEMPLATE_DEFINITIONS);

                return clientOptInput;
            }
        }
    }
}

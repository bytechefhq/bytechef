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

import com.bytechef.cli.command.component.ComponentCommand;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * @author Ivica Cardic
 */
@CommandLine.Command(name = "init", description = "Generates project for a new component)")
public class InitSubcommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ComponentCommand.class);

    private static final String COMPONENT_HANDLER = "ComponentHandler";
    private static final String OPEN_API = "openApi";
    private static final String TEMPLATES_COMPONENT_REST = "templates/component/rest/";

    private static Handlebars handlebars = new Handlebars();

    static {
        handlebars.registerHelper("ifEquals", (Helper<String>) (context, options) -> {
            if (Objects.equals(context, options.param(0))) {
                return options.fn(options.context);
            }

            return null;
        });
    }

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
    private transient String openAPIPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            paramLabel = "output directory",
            description = "where to write the generated files (current dir by default)",
            defaultValue = "")
    private transient String output;

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
        if (StringUtils.isNotEmpty(openAPIPath)) {
            generateOpenAPIComponent();
        }

        return null;
    }

    private void generateOpenAPIComponent() throws IOException {
        if (!openAPIPath.matches("^http(s)?://.*") && !new File(openAPIPath).exists()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(), "The OpenAPI file is not found: " + openAPIPath);
        }

        OpenAPI openAPI = parseOpenAPIFile();

        String componentHandlerDirPath = getComponentHandlerDirPath();

        Files.createDirectories(Paths.get(componentHandlerDirPath));

        writeAbstractComponentHandlerTemplate(openAPI, componentHandlerDirPath);

        writeComponentHandlerTemplate(componentHandlerDirPath);

        writeOpenApiComponentHandlerServiceTemplate();
    }

    private OpenAPI parseOpenAPIFile() {
        SwaggerParseResult result = new OpenAPIParser().readLocation(openAPIPath, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        if (result.getMessages() != null) {
            List<String> messages = result.getMessages();

            messages.forEach(logger::error);
        }

        return openAPI;
    }

    private void writeAbstractComponentHandlerTemplate(OpenAPI openAPI, String componentHandlerDirPath)
            throws IOException {
        Template componentHandlerTemplate = handlebars.compile(TEMPLATES_COMPONENT_REST + "abstract_component_handler");

        try (PrintWriter printWriter = new PrintWriter(
                componentHandlerDirPath + File.separator + "Abstract" + StringUtils.capitalize(componentName)
                        + COMPONENT_HANDLER + ".java",
                StandardCharsets.UTF_8)) {
            componentHandlerTemplate.apply(
                    Map.of(
                            "capitalizedComponentName",
                            StringUtils.capitalize(componentName),
                            "componentName",
                            componentName,
                            OPEN_API,
                            openAPI,
                            "packageName",
                            getPackageName()),
                    printWriter);
        }
    }

    private void writeComponentHandlerTemplate(String componentHandlerDirPath) throws IOException {
        String filename = componentHandlerDirPath + File.separator + StringUtils.capitalize(componentName)
                + COMPONENT_HANDLER + ".java";

        if (!new File(filename).exists()) {
            Template componentHandlerTemplate = handlebars.compile(TEMPLATES_COMPONENT_REST + "component_handler");

            try (PrintWriter printWriter = new PrintWriter(filename, StandardCharsets.UTF_8)) {
                componentHandlerTemplate.apply(
                        Map.of(
                                "capitalizedComponentName",
                                StringUtils.capitalize(componentName),
                                "packageName",
                                getPackageName()),
                        printWriter);
            }
        }
    }

    private String getComponentHandlerDirPath() {
        return getAbsolutePath("src" + File.separator + "main" + File.separator + "java" + File.separator
                + StringUtils.replaceChars(getPackageName(), ".", File.separator));
    }

    private void writeOpenApiComponentHandlerServiceTemplate() throws IOException {
        String servicesDirPath = getAbsolutePath("src" + File.separator + "main" + File.separator + "resources"
                + File.separator + "META-INF" + File.separator + "services");

        Files.createDirectories(Paths.get(servicesDirPath));

        String serviceName = "com.bytechef.hermes.component." + StringUtils.capitalize(OPEN_API) + COMPONENT_HANDLER;

        Template openApiComponentHandlerServiceTemplate = handlebars.compile(TEMPLATES_COMPONENT_REST + serviceName);

        try (PrintWriter printWriter =
                new PrintWriter(servicesDirPath + File.separator + serviceName, StandardCharsets.UTF_8)) {
            openApiComponentHandlerServiceTemplate.apply(
                    Map.of(
                            OPEN_API + COMPONENT_HANDLER + "ClassImpl",
                            getPackageName() + "." + StringUtils.capitalize(componentName) + COMPONENT_HANDLER),
                    printWriter);
        }
    }

    private String getPackageName() {
        return StringUtils.deleteWhitespace(basePackageName == null ? "" : basePackageName + ".")
                + StringUtils.replaceChars(componentName, "-_", ".");
    }

    private String getAbsolutePath(String subPath) {
        File outputDir = new File(output);

        return outputDir.getAbsolutePath() + File.separator + componentName + File.separator + subPath;
    }
}

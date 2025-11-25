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

package com.bytechef.cli.command.component.init.openapi;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ComponentInitOpenApiGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ComponentInitOpenApiGenerator.class);

    private static final ClassName AUTHORIZATION_CLASS_NAME = ClassName.get(
        "com.bytechef.component.definition", "Authorization");
    public static final String COM_BYTECHEF_COMPONENT_PACKAGE = "com.bytechef.component";
    public static final ClassName COMPONENT_DEFINITION_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_COMPONENT_PACKAGE + ".definition", "ComponentDefinition");
    public static final ClassName CONNECTION_DEFINITION_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_COMPONENT_PACKAGE + ".definition", "ConnectionDefinition");
    public static final ClassName COMPONENT_DSL_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_COMPONENT_PACKAGE + ".definition", "ComponentDsl");
    private static final ClassName CONTEXT_HTTP_CLASS = ClassName
        .get("com.bytechef.component.definition", "Context", "Http");
    private static final ClassName OPEN_API_COMPONENT_HANDLER_CLASS = ClassName
        .get("com.bytechef.component", "OpenApiComponentHandler");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            enable(SerializationFeature.INDENT_OUTPUT);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };
    private static final String[] COMPONENT_DIR_NAMES = {
        "action", "constant", "connection", "property", "trigger", "util"
    };

    private final String basePackageName;
    private final String componentName;
    private final GeneratorConfig generatorConfig;
    private final String libsPathname;
    private final OpenAPI openAPI;
    private final String outputPathname;
    private final Set<String> schemas = new HashSet<>();
    private final boolean internalComponent;
    private final int version;
    private final Set<String> oAuth2Scopes = new HashSet<>();
    private final Map<String, String> dynamicOptionsMap = new HashMap<>();
    private final Set<String> dynamicProperties = new HashSet<>();
    private final List<ClassName> aiAgentTools = new ArrayList<>();

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ComponentInitOpenApiGenerator(
        String basePackageName, String componentName, int version, boolean internalComponent,
        String openApiPath, String outputPathname, String libsPathname) throws IOException {

        this.basePackageName = basePackageName;
        this.componentName = componentName;
        this.libsPathname = libsPathname;
        this.internalComponent = internalComponent;
        this.openAPI = parseOpenAPIFile(openApiPath);
        this.outputPathname = outputPathname;
        this.version = version;

        File directory = new File(openApiPath).getParentFile();

        File configFile = new File(directory.getPath() + File.separator + "generator-config.json");

        if (configFile.exists()) {
            try (JsonParser jsonParser = OBJECT_MAPPER.createParser(configFile)) {
                generatorConfig = jsonParser.readValueAs(GeneratorConfig.class);
            }
        } else {
            generatorConfig = new GeneratorConfig();
        }
    }

    public void generate() throws Exception {
        Path sourceMainJavaDirPath = Files.createDirectories(
            Paths.get(getAbsolutePathname("src" + File.separator + "main" + File.separator + "java")));

        int schemaSize = schemas.size();

        Path componentHandlerSourcePath = writeAbstractComponentHandlerSource(sourceMainJavaDirPath);

        while (schemaSize != schemas.size()) {
            schemaSize = schemas.size();

            checkComponentSchemaSources(schemas);
        }

        writeComponentSchemaSources(schemas, sourceMainJavaDirPath);
        writeComponentHandlerSource(sourceMainJavaDirPath);

        // Utils must be generated before Test classes
        if (!dynamicOptionsMap.isEmpty() || !dynamicProperties.isEmpty()) {
            writeAbstractUtils(sourceMainJavaDirPath);
            writeUtils(sourceMainJavaDirPath);
        }

        if (internalComponent) {
            Path sourceTestJavaDirPath = Files.createDirectories(
                Paths.get(getAbsolutePathname("src" + File.separator + "test" + File.separator + "java")));

            writeAbstractComponentHandlerTest(sourceTestJavaDirPath);
            writeComponentHandlerTest(sourceTestJavaDirPath);
            writeComponentHandlerDefinition(componentHandlerSourcePath, getPackageName(), version);
        }
    }

    private OpenAPI parseOpenAPIFile(String openApiPath) {
        SwaggerParseResult result = new OpenAPIParser().readLocation(openApiPath, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        if (result.getMessages() != null) {
            List<String> messages = result.getMessages();

            messages.forEach(logger::error);
        }

        return openAPI;
    }

    private CodeBlock addDynamicProperty(
        String propertyName, String propertyDescription, Boolean required, Map<String, Object> extensionMap,
        boolean bodySchema) {

        CodeBlock.Builder builder = CodeBlock.builder();

        propertyName = StringUtils.isEmpty(propertyName) ? "__item" : propertyName;

        Object dynamicProperties = extensionMap.get("x-dynamic-properties");

        if (dynamicProperties.equals(true)) {
            builder.add("dynamicProperties($S)", propertyName);

            builder.add(
                ".properties(($T.PropertiesFunction)$T::get" + buildOptionsFunctionsName(propertyName) +
                    "Properties)",
                ClassName.get("com.bytechef.component.definition", "ActionDefinition"),
                ClassName.get(
                    "com.bytechef.component." + componentName + ".util",
                    getComponentClassName(componentName) + "Utils"));

            this.dynamicProperties.add(buildOptionsFunctionsName(propertyName));

            if (extensionMap.get("x-dynamic-properties-dependency") instanceof List<?> dependencies) {
                List<String> allDependencies = dependencies.stream()
                    .map(object -> "\"" + object + "\"")
                    .toList();

                builder.add(".propertiesLookupDependsOn($L)", String.join(",", allDependencies));
            }
        }

        if (propertyDescription != null) {
            builder.add(".description($S)", propertyDescription);
        }

        if (required != null) {
            builder.add(".required($L)", required);
        }

        if (bodySchema) {
            builder.add(
                """
                    .metadata(
                       $T.of(
                         "type", PropertyType.BODY
                       )
                    )
                    """,
                Map.class);
        }

        return builder.build();
    }

    private void addOperationItem(List<OperationItem> operationItems, Operation operation, String method, String path) {
        if (operation != null) {
            operationItems.add(new OperationItem(operation, method, path));
        }
    }

    private Map<String, List<OperationItem>> getOperationItemsMap(io.swagger.v3.oas.models.Paths paths) {
        Map<String, List<OperationItem>> operationItemsMap = new LinkedHashMap<>();
        List<OperationItem> operationItems = extractOperationItems(paths);

        for (OperationItem operationItem : operationItems) {
            String tag = determineTag(operationItem);

            operationItemsMap
                .computeIfAbsent(tag, k -> new ArrayList<>())
                .add(operationItem);
        }

        return operationItemsMap;
    }

    private JavaFile.Builder addStaticImport(JavaFile.Builder builder) {
        return builder
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "ADD_TO")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "AUTHORIZATION_URL")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "CLIENT_ID")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "CLIENT_SECRET")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "HEADER_PREFIX")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "KEY")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "PASSWORD")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "REFRESH_URL")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "SCOPES")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "TOKEN")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "TOKEN_URL")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "USERNAME")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "VALUE")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "action")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "array")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "authorization")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "bool")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "component")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "connection")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "date")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "dateTime")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "dynamicProperties")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "fileEntry")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "integer")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "nullable")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "number")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "object")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "option")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "outputSchema")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "sampleOutput")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "string")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "time")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "tool")
            .addStaticImport(CONNECTION_DEFINITION_CLASS_NAME, "BASE_URI")
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "ApiTokenLocation", "AuthorizationType")
            .addStaticImport(CONTEXT_HTTP_CLASS, "BodyContentType", "ResponseType")
            .addStaticImport(OPEN_API_COMPONENT_HANDLER_CLASS, "PropertyType");
    }

    private static String buildOptionsFunctionsName(String propertyName) {
        return Arrays.stream(StringUtils.split(propertyName, '_'))
            .flatMap(item -> Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(item)))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(""));
    }

    private static String buildPropertyLabel(String propertyName) {
        return Arrays.stream(StringUtils.split(propertyName, '_'))
            .flatMap(item -> Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(item)))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    public static String buildPropertyNameFromTitle(String title) {
        String[] split = StringUtils.split(title, ' ');

        String collect = Arrays.stream(split)
            .skip(1)
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(""));

        return split[0].toLowerCase() + collect;
    }

    private void checkAdditionalProperties(
        String propertyName, String propertyDescription, Boolean required, Schema<?> schema, boolean outputSchema,
        String type, CodeBlock.Builder builder) {

        if (!StringUtils.isEmpty(propertyName) && !outputSchema) {
            builder.add(
                ".label($S)",
                StringUtils.isEmpty(schema.getTitle()) ? buildPropertyLabel(propertyName.replace("__", ""))
                    : schema.getTitle());
        }

        if (propertyDescription != null) {
            builder.add(".description($S)", propertyDescription);
        }

        if (schema.getEnum() != null) {
            List<CodeBlock> codeBlocks = getEnumOptionsCodeBlocks(schema);

            if (!Objects.equals(type, "boolean")) {
                builder.add(".options($L)", codeBlocks.stream()
                    .collect(CodeBlock.joining(",")));
            }
        }

        if (schema.getDefault() != null) {
            if (Objects.equals(type, "string")) {
                builder.add(".defaultValue($S)", schema.getDefault());
            } else {
                builder.add(".defaultValue($L)", schema.getDefault());
            }
        }

        if (required != null && !StringUtils.isEmpty(propertyName)) {
            builder.add(".required($L)", required);
        }

        if (schema.getExample() != null) {
            if (Objects.equals(type, "string")) {
                builder.add(".exampleValue($S)", schema.getExample());
            } else {
                builder.add(".exampleValue($L)", schema.getExample());
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void checkComponentSchemaSources(Set<String> schemas) {
        Components components = openAPI.getComponents();

        if (components != null) {
            Map<String, Schema> schemaMap = components.getSchemas();

            if (schemaMap != null) {
                for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
                    if (!schemas.contains(entry.getKey())) {
                        continue;
                    }

                    getObjectPropertiesCodeBlock(null, entry.getValue(), false, openAPI);
                }
            }
        }
    }

    private Path compileComponentHandlerSource(Path sourcePath) throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        List<String> javacOpts = new ArrayList<>();

        javacOpts.add("-classpath");

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
            Path.of(this.libsPathname == null ? "libs" : this.libsPathname))) {
            javacOpts.add(
                StreamSupport.stream(directoryStream.spliterator(), false)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".jar"))
                    .collect(Collectors.joining(File.pathSeparator)));
        }

        Path parentPath = sourcePath.getParent();

        for (String dirName : COMPONENT_DIR_NAMES) {
            Path dirPath = parentPath.resolve(dirName);

            File dirFile = dirPath.toFile();

            File[] files = dirFile.listFiles((curDir, name) -> {
                name = name.toLowerCase();

                return name.endsWith(".java");
            });

            if (files != null) {
                for (File file : files) {
                    javacOpts.add(dirFile.getAbsolutePath() + "/" + file.getName());
                }
            }
        }

        Path tempDirPath;

        try {
            tempDirPath = Files.createTempDirectory("openapi_component_classes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        javacOpts.add("-d");
        javacOpts.add(tempDirPath.toString());

        String simpleClassName = getComponentHandlerClassName(componentName);

        javacOpts.add(sourcePath.getParent() + "/Abstract" + simpleClassName + ".java");
        javacOpts.add(sourcePath.getParent() + "/" + simpleClassName + ".java");
        javacOpts.add(
            sourcePath.getParent() + "/connection/" + getComponentClassName(componentName) + "Connection.java");

        if (!dynamicOptionsMap.isEmpty()) {
            javacOpts.add(
                sourcePath.getParent() + "/util/" + "Abstract" + getComponentClassName(componentName) + "Utils.java");
        }

        javaCompiler.run(null, null, null, javacOpts.toArray(new String[0]));

        return tempDirPath;
    }

    private void collectOAuthScopes(List<SecurityRequirement> securityRequirements) {
        Components components = openAPI.getComponents();

        if (components != null) {
            Map<String, SecurityScheme> securitySchemeMap = components.getSecuritySchemes();
            List<String> oauth2SecuritySchemeNames = new ArrayList<>();

            for (Map.Entry<String, SecurityScheme> entry : securitySchemeMap.entrySet()) {
                SecurityScheme securityScheme = entry.getValue();

                if (securityScheme.getType() == SecurityScheme.Type.OAUTH2) {
                    oauth2SecuritySchemeNames.add(entry.getKey());
                }
            }

            for (SecurityRequirement securityRequirement : securityRequirements) {
                for (Map.Entry<String, List<String>> entry : securityRequirement.entrySet()) {
                    if (oauth2SecuritySchemeNames.contains(entry.getKey())) {
                        oAuth2Scopes.addAll(entry.getValue());
                    }
                }
            }
        }
    }

    private static <T> Object convert(Object fromValue, TypeReference<T> toValueType) {
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (IllegalArgumentException e1) {
            if (logger.isTraceEnabled()) {
                logger.trace(e1.getMessage(), e1);
            }
        }

        return null;
    }

    private OpenApiComponentHandler createComponentHandler(Path classPath, String className) throws Exception {
        File classFile = classPath.toFile();

        URI classURI = classFile.toURI();

        URL[] classUrls = new URL[] {
            classURI.toURL()
        };

        URLClassLoader classLoader = URLClassLoader.newInstance(
            classUrls, OpenApiComponentHandler.class.getClassLoader());

        @SuppressWarnings("unchecked")
        Class<OpenApiComponentHandler> clazz = (Class<OpenApiComponentHandler>) Class.forName(
            className, true, classLoader);

        Constructor<OpenApiComponentHandler> declaredConstructor = clazz.getDeclaredConstructor();

        return declaredConstructor.newInstance();
    }

    private MethodSpec createOptionsMethod(String propertyName, ClassName returnType) {
        return MethodSpec.methodBuilder("get" + propertyName + "Options")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ParameterSpec.builder(Parameters.class, "inputParameters")
                .build())
            .addParameter(ParameterSpec.builder(Parameters.class, "connectionParameters")
                .build())
            .addParameter(ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                    ClassName.get(String.class)),
                "lookupDependsOnPaths")
                .build())
            .addParameter(ParameterSpec.builder(String.class, "searchText")
                .build())
            .addParameter(ParameterSpec.builder(Context.class, "context")
                .build())
            .returns(ParameterizedTypeName.get(
                ClassName.get("java.util", "List"),
                ParameterizedTypeName.get(
                    ClassName.get("com.bytechef.component.definition", "Option"),
                    returnType)))
            .addStatement("\n return List.of()")
            .build();
    }

    private MethodSpec createPropertiesMethod(String propertyName) {
        return MethodSpec.methodBuilder("get" + propertyName + "Properties")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ParameterSpec.builder(Parameters.class, "inputParameters")
                .build())
            .addParameter(ParameterSpec.builder(Parameters.class, "connectionParameters")
                .build())
            .addParameter(ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                    ClassName.get(String.class)),
                "lookupDependsOnPaths")
                .build())
            .addParameter(ParameterSpec.builder(Context.class, "context")
                .build())
            .returns(ParameterizedTypeName.get(
                ClassName.get("java.util", "List"),
                ParameterizedTypeName.get(
                    ClassName.get(
                        "com.bytechef.component.definition", "ComponentDsl", "ModifiableValueProperty"),
                    WildcardTypeName.subtypeOf(Object.class), WildcardTypeName.subtypeOf(Object.class))))
            .addStatement("\n return List.of()")
            .build();
    }

    private String deleteWhitespace(final String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }

        final int sz = str.length();
        final char[] chs = new char[sz];

        int count = 0;
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs[count++] = str.charAt(i);
            }
        }

        if (count == sz) {
            return str;
        }

        if (count == 0) {
            return "";
        }

        return new String(chs, 0, count);
    }

    private String determineTag(OperationItem operationItem) {
        List<String> tags = operationItem.operation()
            .getTags();

        return (tags != null && !tags.isEmpty()) ? tags.getFirst() : "unnamed";
    }

    private List<OperationItem> extractOperationItems(io.swagger.v3.oas.models.Paths paths) {
        List<OperationItem> operationItems = new ArrayList<>();

        paths.forEach((path, pathItem) -> {
            addOperationItem(operationItems, pathItem.getDelete(), "DELETE", path);
            addOperationItem(operationItems, pathItem.getHead(), "HEAD", path);
            addOperationItem(operationItems, pathItem.getGet(), "GET", path);
            addOperationItem(operationItems, pathItem.getPatch(), "PATCH", path);
            addOperationItem(operationItems, pathItem.getPost(), "POST", path);
            addOperationItem(operationItems, pathItem.getPut(), "PUT", path);
        });

        return operationItems;
    }

    private Map<String, List<OperationItem>> filterOperationItemsMap(Map<String, List<OperationItem>> operationsMap) {
        Map<String, List<OperationItem>> filteredOperationsMap;
        List<String> operations = generatorConfig.openApi.operations;

        if (operations.isEmpty()) {
            filteredOperationsMap = operationsMap;
        } else {
            filteredOperationsMap = new LinkedHashMap<>();

            for (Map.Entry<String, List<OperationItem>> operationItemsEntry : operationsMap.entrySet()) {
                List<OperationItem> curOperationItems = operationItemsEntry.getValue();

                curOperationItems = curOperationItems.stream()
                    .filter(operationItem -> operations.stream()
                        .anyMatch(operationId -> Objects.equals(operationItem.getOperationId(), operationId)))
                    .toList();

                if (!curOperationItems.isEmpty()) {
                    filteredOperationsMap.put(operationItemsEntry.getKey(), curOperationItems);
                }
            }
        }

        return filteredOperationsMap;
    }

    private String getAbsolutePathname(String subPath) {
        return outputPathname + File.separator + componentName + File.separator + subPath;
    }

    private CodeBlock getActionCodeBlock(OperationItem operationItem, OpenAPI openAPI) {
        Operation operation = operationItem.operation();
        String method = operationItem.method();

        OutputEntry outputEntry = getOutputEntry(operation);
        PropertiesEntry propertiesEntry = getPropertiesEntry(operation, openAPI);

        CodeBlock.Builder builder = CodeBlock.builder();

        CodeBlock.Builder metadataBuilder = getMetadataBuilder(operationItem, method, propertiesEntry);

        if (outputEntry != null && outputEntry.isDynamic) {
            metadataBuilder.add(outputEntry.outputSchemaCodeBlock());
        }

        builder.add(
            """
                action($S)
                    .title($S)
                    .description($S)
                    .metadata(
                        $T.of(
                            $L
                        )
                    )
                    .properties($L)
                """,
            operation.getOperationId(),
            operation.getSummary(),
            operation.getDescription(),
            Map.class,
            metadataBuilder.build(),
            propertiesEntry.propertiesCodeBlock());

        CodeBlock outputSchemaCodeBlock = outputEntry == null ? null : outputEntry.outputSchemaCodeBlock();
        CodeBlock sampleOutputCodeBlock = outputEntry == null ? null : outputEntry.sampleOutputCodeBlock();

        if (outputEntry != null && outputEntry.isDynamic) {
            builder.add(".output()");
        } else if (outputSchemaCodeBlock != null && !outputSchemaCodeBlock.isEmpty()) {
            if (sampleOutputCodeBlock == null || sampleOutputCodeBlock.isEmpty()) {
                builder.add(".output(outputSchema($L))", outputSchemaCodeBlock);
            } else {
                builder.add(".output(outputSchema($L), sampleOutput($L))", outputSchemaCodeBlock,
                    sampleOutputCodeBlock);
            }
        }

        return builder.build();
    }

    private CodeBlock getActionsCodeBlock(Path componentHandlerDirPath, OpenAPI openAPI) throws IOException {
        Map<String, List<OperationItem>> operationsMap = filterOperationItemsMap(
            getOperationItemsMap(openAPI.getPaths()));

        List<CodeBlock> codeBlocks = new ArrayList<>();

        for (Map.Entry<String, List<OperationItem>> operationItemsEntry : operationsMap.entrySet()) {
            for (OperationItem operationItem : operationItemsEntry.getValue()) {
                CodeBlock actionCodeBlock = getActionCodeBlock(operationItem, openAPI);

                ClassName className = ClassName.get(
                    getPackageName() + ".action",
                    getComponentClassName(componentName) + StringUtils.capitalize(operationItem.getOperationId()) +
                        "Action");

                writeComponentActionSource(className, actionCodeBlock, componentHandlerDirPath);

                codeBlocks.add(CodeBlock.of("$T.ACTION_DEFINITION", className));

                if (generatorConfig.openApi.oAuth2Scopes.isEmpty() && operationItem.operation.getSecurity() != null) {
                    collectOAuthScopes(operationItem.operation.getSecurity());
                }

                Operation operation = operationItem.operation();
                Map<String, Object> extensions = operation.getExtensions();

                if (extensions != null && Boolean.TRUE.equals(extensions.get("x-ai-agent-tool"))) {
                    aiAgentTools.add(className);
                }
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
    }

    private CodeBlock getAdditionalPropertiesCodeBlock(String propertyName, Schema<?> schema, boolean outputSchema) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (schema.getAdditionalProperties() instanceof Boolean additionalProperties) {
            if (additionalProperties) {
                builder.add(
                    """
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                        """);
            }
        } else {
            Schema<?> additionalPropertiesSchema = (Schema<?>) schema.getAdditionalProperties();

            if (StringUtils.isEmpty(additionalPropertiesSchema.get$ref())) {
                Map<String, ?> additionalPropertiesSchemaProperties = additionalPropertiesSchema.getProperties();

                if (additionalPropertiesSchemaProperties == null || additionalPropertiesSchemaProperties.isEmpty()) {
                    builder.add(
                        ".additionalProperties($L())", getAdditionalPropertiesItemType(additionalPropertiesSchema));
                } else {
                    builder.add(
                        ".additionalProperties($L().properties($L))",
                        getAdditionalPropertiesItemType(additionalPropertiesSchema),
                        getObjectPropertiesCodeBlock(propertyName, additionalPropertiesSchema, outputSchema, openAPI));
                }
            } else {
                String ref = additionalPropertiesSchema.get$ref();

                String curSchemaName = ref.replace("#/components/schemas/", "");

                schemas.add(curSchemaName);

                builder.add(
                    ".additionalProperties(object().properties($L))",
                    CodeBlock.of("$T.PROPERTIES", getPropertiesClassName(curSchemaName)));
            }
        }

        if (!outputSchema && propertyName != null) {
            builder.add(".placeholder($S)", "Add to " + buildPropertyLabel(propertyName.replace("__", "")));
        }

        return builder.build();
    }

    @SuppressWarnings("rawtypes")
    private String getAdditionalPropertiesItemType(Schema additionalPropertiesSchema) {
        String additionalPropertiesSchemaType = StringUtils.isEmpty(additionalPropertiesSchema.getType()) ? "object"
            : additionalPropertiesSchema.getType();

        return switch (additionalPropertiesSchemaType) {
            case "array" -> "array";
            case "boolean" -> "bool";
            case "integer" -> "integer";
            case "number" -> "number";
            case "object" -> "object";
            case "string" -> {
                if (StringUtils.isEmpty(additionalPropertiesSchema.getFormat())) {
                    yield "string";
                } else if (Objects.equals(additionalPropertiesSchema.getFormat(), "date")) {
                    yield "date";
                } else if (Objects.equals(additionalPropertiesSchema.getFormat(), "date-date")) {
                    yield "date-time";
                } else {
                    throw new IllegalArgumentException(
                        "Unsupported schema type format: " + additionalPropertiesSchema.getFormat());
                }
            }
            default -> throw new IllegalArgumentException(
                "Unsupported schema type: " + additionalPropertiesSchema.getType());
        };
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private CodeBlock getAllOfSchemaCodeBlock(
        String name, String description, List<Schema> allOfSchemas, boolean outputSchema, OpenAPI openAPI) {

        Map<String, Schema> allOfProperties = getAllOfSchemaProperties(name, description, allOfSchemas);
        List<String> allOfRequired = new ArrayList<>();

        for (Schema allOfSchema : allOfProperties.values()) {
            if (allOfSchema.getRequired() != null) {
                allOfRequired.addAll(allOfSchema.getRequired());
            }
        }

        return getPropertiesSchemaCodeBlock(allOfProperties, allOfRequired, outputSchema, openAPI);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private Map<String, Schema> getAllOfSchemaProperties(String name, String description, List<Schema> allOfSchemas) {
        Map<String, Schema> allOfProperties = new HashMap<>();

        for (Schema allOfSchema : allOfSchemas) {
            if (allOfSchema.getProperties() != null || allOfSchema.getAllOf() != null) {
                if (allOfSchema.getProperties() != null) {
                    allOfProperties.putAll(allOfSchema.getProperties());
                }

                if (allOfSchema.getAllOf() != null) {
                    allOfProperties.putAll(getAllOfSchemaProperties(name, description, allOfSchema.getAllOf()));
                }
            } else {
                allOfProperties.put(name, allOfSchema.description(description));
            }
        }

        return allOfProperties;
    }

    private CodeBlock getAuthorizationApiKeyCodeBlock(SecurityScheme securityScheme) {
        CodeBlock.Builder builder = CodeBlock.builder();

        SecurityScheme.In in = securityScheme.getIn();

        CodeBlock apiKeyCodeBlock;

        if (StringUtils.isEmpty(securityScheme.getName()) || Objects.equals(securityScheme.getName(), "api_token")) {
            apiKeyCodeBlock = CodeBlock.builder()
                .build();
        } else {
            apiKeyCodeBlock = CodeBlock.of(
                """
                    string(KEY)
                         .label($S)
                         .required($L)
                         .defaultValue($L)
                         .hidden($L),""",
                "Key",
                true,
                CodeBlock.of("$S", securityScheme.getName()),
                true);
        }

        CodeBlock addToCodeBlock;

        if (in == SecurityScheme.In.HEADER) {
            addToCodeBlock = CodeBlock.builder()
                .build();
        } else {
            addToCodeBlock = CodeBlock.of(
                """
                    ,string(ADD_TO)
                        .label($S)
                        .required($L)
                        .defaultValue($L)
                        .hidden($L)
                    """,
                "Add to",
                true,
                CodeBlock.of("$T.ApiTokenLocation.QUERY_PARAMETERS.name()", AUTHORIZATION_CLASS_NAME),
                true);
        }

        Map<String, Object> extensions = securityScheme.getExtensions();

        String label = "Value";

        if (extensions != null && extensions.get("x-title") != null) {
            label = (String) extensions.get("x-title");
        }

        builder.add(
            """
                authorization(AuthorizationType.API_KEY)
                    .title($S)
                    .properties(
                        $L
                        string(VALUE)
                            .label($S)
                            .required($L)
                        $L
                    )
                """,
            "API Key",
            apiKeyCodeBlock,
            label,
            true,
            addToCodeBlock);

        return builder.build();
    }

    private CodeBlock getAuthorizationBasicCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(AuthorizationType.BASIC_AUTH)
                    .title($S)
                    .properties(
                        string(USERNAME)
                            .label($S)
                            .required($L),
                        string(PASSWORD)
                            .label($S)
                            .required($L)
                    )
                """,
            "Basic Auth",
            "Username",
            true,
            "Password",
            true);

        return builder.build();
    }

    private CodeBlock getAuthorizationBearerCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(AuthorizationType.BEARER_TOKEN)
                    .title($S)
                    .properties(
                        string(TOKEN)
                            .label($S)
                            .required($L)
                    )
                """,
            "Bearer Token",
            "Token",
            true);

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2AuthorizationCodeCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();
        String oAuth2Scopes = getOAuth2Scopes(oAuthFlow.getScopes());

        builder.add(
            """
                authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .authorizationUrl((connectionParameters, context) -> $S)
                    $L.tokenUrl((connectionParameters, context) -> $S)
                """,
            "OAuth2 Authorization Code",
            "Client Id",
            true,
            "Client Secret",
            true,
            oAuthFlow.getAuthorizationUrl(),
            StringUtils.isEmpty(oAuth2Scopes)
                ? CodeBlock.builder()
                    .build()
                : CodeBlock.of(
                    ".scopes((connection, context) -> $T.of($L))", List.class, getOAuth2Scopes(oAuthFlow.getScopes())),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl((connectionParameters, context) -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2ClientCredentialsCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .scopes((connectionParameters, context) -> $T.of($L))
                    .tokenUrl((connectionParameters, context) -> $S)
                """,
            "Client Credentials",
            "Client Id",
            true,
            "OAuth2 Client Secret",
            true,
            List.class,
            getOAuth2Scopes(oAuthFlow.getScopes()),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl((connectionParameters, context) -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2ImplicitCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(AuthorizationType.OAUTH2_IMPLICIT_CODE)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .authorizationUrl((connectionParameters, context) -> $S)
                    .scopes((connectionParameters, context) -> $T.of($L))
                """,
            "OAuth2 Implicit",
            "Client Id",
            true,
            "Client Secret",
            true,
            oAuthFlow.getAuthorizationUrl(),
            List.class,
            getOAuth2Scopes(oAuthFlow.getScopes()));

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl((connectionParameters, context) -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2PasswordCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(AuthorizationType.OAUTH2_RESOURCE_OWNER_PASSWORD)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .scopes((connectionParameters, context) -> $T.of($L))
                    .tokenUrl((connectionParameters, context) -> $S)
                """,
            "OAuth2 Resource Owner Password",
            "Client Id",
            true,
            "Client Secret",
            true,
            List.class,
            oAuthFlow.getRefreshUrl(),
            getOAuth2Scopes(oAuthFlow.getScopes()),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl((connectionParameters, context) -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationsCodeBlock(Map<String, SecurityScheme> securitySchemeMap) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        if (securitySchemeMap == null || securitySchemeMap.isEmpty()) {
            codeBlocks.add(CodeBlock.of("null"));
        } else {
            for (Map.Entry<String, SecurityScheme> entry : securitySchemeMap.entrySet()) {
                SecurityScheme securityScheme = entry.getValue();

                if (securityScheme.getType() == SecurityScheme.Type.APIKEY) {
                    codeBlocks.add(getAuthorizationApiKeyCodeBlock(securityScheme));
                } else if (securityScheme.getType() == SecurityScheme.Type.HTTP) {
                    String scheme = securityScheme.getScheme();

                    if (Objects.equals(scheme, "basic")) {
                        codeBlocks.add(getAuthorizationBasicCodeBlock());
                    } else if (Objects.equals(scheme, "bearer")) {
                        codeBlocks.add(getAuthorizationBearerCodeBlock());
                    } else {
                        throw new IllegalArgumentException("Security scheme: %s not supported".formatted(scheme));
                    }
                } else if (securityScheme.getType() == SecurityScheme.Type.OAUTH2) {
                    OAuthFlows flows = securityScheme.getFlows();

                    OAuthFlow oAuthFlow = flows.getAuthorizationCode();

                    if (oAuthFlow != null) {
                        codeBlocks.add(getAuthorizationOAuth2AuthorizationCodeCodeBlock(oAuthFlow));
                    }

                    oAuthFlow = flows.getClientCredentials();

                    if (oAuthFlow != null) {
                        codeBlocks.add(getAuthorizationOAuth2ClientCredentialsCodeBlock(oAuthFlow));
                    }

                    oAuthFlow = flows.getImplicit();

                    if (oAuthFlow != null) {
                        codeBlocks.add(getAuthorizationOAuth2ImplicitCodeBlock(oAuthFlow));
                    }

                    oAuthFlow = flows.getPassword();

                    if (oAuthFlow != null) {
                        codeBlocks.add(getAuthorizationOAuth2PasswordCodeBlock(oAuthFlow));
                    }
                } else {
                    throw new IllegalArgumentException(
                        "Security scheme type=%s not supported".formatted(securityScheme.getType()));
                }
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
    }

    private static CodeBlock getBaseUriCodeBlock(List<Server> servers) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (servers == null || servers.isEmpty()) {
            builder.add("null");
        } else {
            if (servers.size() == 1) {
                Server server = servers.getFirst();

                if (!StringUtils.isEmpty(server.getUrl()) && !Objects.equals(server.getUrl(), "/")) {
                    builder.add(".baseUri((connectionParameters, context) -> $S)", server.getUrl());
                }
            } else {
                List<CodeBlock> codeBlocks = new ArrayList<>();

                for (Server server : servers) {
                    codeBlocks.add(CodeBlock.of("option($L)", server.getUrl()));
                }

                builder.add(
                    """
                        .properties(
                            string(BASE_URI)
                                .label($S)
                                .options($L)"
                        """,
                    "Base URI",
                    codeBlocks.stream()
                        .collect(CodeBlock.joining(",")));

                Server server = servers.getFirst();

                builder.add(".defaultValue($S)", server.getUrl());
                builder.add(")");
            }
        }

        return builder.build();
    }

    private String getComponentClassName(String componentName) {
        String[] items = componentName.split("-");

        return Arrays.stream(items)
            .map(StringUtils::capitalize)
            .collect(Collectors.joining());
    }

    private CodeBlock getComponentCodeBlock(Path componentHandlerDirPath) throws IOException {
        CodeBlock.Builder builder = CodeBlock.builder();

        Info info = openAPI.getInfo();

        String title = info.getTitle();

        if (StringUtils.isEmpty(title)) {
            title = getComponentClassName(componentName);
        }

        String name = getComponentClassName(componentName);

        builder.add(
            """
                modifyComponent(
                    component($S)
                        .title($S)
                        .description($S)
                    )
                    .actions(modifyActions($L))
                """,

            StringUtils.uncapitalize(name),
            title, info.getDescription(),
            getActionsCodeBlock(componentHandlerDirPath, openAPI));

        CodeBlock codeBlock = getConnectionCodeBlock(openAPI, componentHandlerDirPath);

        if (!codeBlock.isEmpty()) {
            builder.add(".connection(modifyConnection($L))", codeBlock);
        }

        if (!aiAgentTools.isEmpty()) {
            List<CodeBlock> codeBlocks = new ArrayList<>();

            for (ClassName className : aiAgentTools) {
                codeBlocks.add(CodeBlock.of("tool($T.ACTION_DEFINITION)", className));
            }

            builder.add(".clusterElements(modifyClusterElements($L))", codeBlocks.stream()
                .collect(CodeBlock.joining(",")));
        }

        builder.add(".triggers(getTriggers())");

        return builder.build();
    }

    private String getComponentHandlerClassName(String componentName) {
        return getComponentClassName(componentName) + "ComponentHandler";
    }

    private CodeBlock getConnectionCodeBlock(OpenAPI openAPI, Path componentHandlerDirPath) throws IOException {
        CodeBlock.Builder builder = CodeBlock.builder();

        Components components = openAPI.getComponents();

        if (components != null) {
            Map<String, SecurityScheme> securitySchemeMap = components.getSecuritySchemes();
            List<Server> servers = openAPI.getServers();

            if (!((securitySchemeMap == null || securitySchemeMap.isEmpty()) &&
                (servers == null || servers.isEmpty()))) {

                CodeBlock connectionCodeBlock = CodeBlock.of(
                    """
                        connection()
                            $L.authorizations($L)
                        """,
                    getBaseUriCodeBlock(servers),
                    getAuthorizationsCodeBlock(securitySchemeMap));

                ClassName className = ClassName.get(
                    getPackageName() + ".connection", getComponentClassName(componentName) + "Connection");

                writeComponentConnectionSource(className, connectionCodeBlock, componentHandlerDirPath);

                builder.add(CodeBlock.of("$T.CONNECTION_DEFINITION", className));
            }
        }

        return builder.build();
    }

    private List<CodeBlock> getEnumOptionsCodeBlocks(Schema<?> schema) {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        List<?> enums = schema.getEnum()
            .stream()
            .filter(Objects::nonNull)
            .toList();

        for (Object item : enums) {
            if (item instanceof String) {
                codeBlocks.add(CodeBlock.of("option($S, $S)", StringUtils.capitalize(item.toString()), item));
            } else {
                codeBlocks.add(
                    CodeBlock.of(
                        "option($S, $L$L)",
                        StringUtils.capitalize(item.toString()), item,
                        switch (schema.getType()) {
                            case "number" -> "D";
                            case "integer" -> Objects.equals(schema.getFormat(), "int64") ? "L" : "";
                            default -> "";
                        }));
            }
        }

        return codeBlocks;
    }

    private static CodeBlock.Builder getMetadataBuilder(
        OperationItem operationItem, String method, PropertiesEntry propertiesEntry) {

        CodeBlock.Builder metadataBuilder = CodeBlock.builder();

        metadataBuilder.add(
            """
                "method", $S,
                "path", $S
                """,
            method,
            operationItem.path);

        if (propertiesEntry.bodyContentType != null) {
            metadataBuilder.add(
                """
                    ,"bodyContentType", BodyContentType.$L
                    ,"mimeType", $S
                    """,
                propertiesEntry.bodyContentType,
                propertiesEntry.mimeType);
        }

        return metadataBuilder;
    }

    private String getMimeType(Set<Map.Entry<String, MediaType>> entries) {
        String mimeType;

        // Check if there is application/json as body content type

        if (entries.stream()
            .map(Map.Entry::getKey)
            .anyMatch(curBodyContentType -> Objects.equals(curBodyContentType, "application/json"))) {

            mimeType = "application/json";
        } else {

            // else use the first body content type

            Iterator<Map.Entry<String, MediaType>> iterator = entries.iterator();

            Map.Entry<String, MediaType> firstEntry = iterator.next();

            mimeType = firstEntry.getKey();
        }

        return mimeType;
    }

    private String getOAuth2Scopes(Scopes scopes) {
        Collection<String> scopeNames;

        if (generatorConfig.openApi.oAuth2Scopes.isEmpty()) {
            if (oAuth2Scopes.isEmpty()) {
                scopeNames = scopes.keySet();
            } else {
                scopeNames = oAuth2Scopes;
            }
        } else {
            scopeNames = generatorConfig.openApi.oAuth2Scopes;
        }

        return String.join(
            ",", scopeNames.stream()
                .map(scope -> "\"" + scope + "\"")
                .toList());
    }

    private CodeBlock getObjectPropertiesCodeBlock(
        String name, Schema<?> schema, boolean outputSchema, OpenAPI openAPI) {

        List<CodeBlock> codeBlocks = new ArrayList<>();

        if (schema.getProperties() != null) {
            codeBlocks.add(
                getPropertiesSchemaCodeBlock(
                    schema.getProperties(), schema.getRequired() == null ? List.of() : schema.getRequired(),
                    outputSchema, openAPI));
        }

        if (schema.getAllOf() != null) {
            codeBlocks.add(
                getAllOfSchemaCodeBlock(name, schema.getDescription(), schema.getAllOf(), outputSchema, openAPI));
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
    }

    private OutputEntry getOutputEntry(Operation operation) {
        ApiResponse apiResponse = null;
        ApiResponses apiResponses = operation.getResponses();
        OutputEntry outputEntry = null;

        for (String responseCode : List.of("200", "201", "202", "default")) {
            apiResponse = apiResponses.get(responseCode);

            if (apiResponse != null) {
                break;
            }
        }

        if (apiResponse != null) {
            if (apiResponse.getExtensions() != null) {
                Map<String, Object> extensions = apiResponse.getExtensions();

                if (extensions != null) {
                    Object dynamicOutput = extensions.get("x-dynamic-output");

                    if (dynamicOutput.equals(true)) {

                        Content content = apiResponse.getContent();

                        Set<Map.Entry<String, MediaType>> entries = content.entrySet();

                        if (!entries.isEmpty()) {
                            String mimeType = getMimeType(entries);
                            String responseType = switch (mimeType) {
                                case "application/json" -> "JSON";
                                case "application/xml" -> "XML";
                                case "application/octet-stream" -> "BINARY";
                                default -> "TEXT";
                            };

                            CodeBlock.Builder codeBlock = CodeBlock.builder();

                            codeBlock.add(
                                """
                                    ,"responseType", ResponseType.$L
                                    """,
                                responseType);

                            outputEntry = new OutputEntry(codeBlock.build(), null, true);
                        }
                    }
                }
            } else if (apiResponse.getExtensions() == null && apiResponse.getContent() != null) {
                Content content = apiResponse.getContent();

                Set<Map.Entry<String, MediaType>> entries = content.entrySet();

                if (!entries.isEmpty()) {
                    String mimeType = getMimeType(entries);

                    MediaType mediaType = content.get(mimeType);

                    outputEntry = new OutputEntry(
                        getOutputSchemaCodeBlock(mimeType, mediaType),
                        getSampleOutputCodeBlock(mediaType.getExample()),
                        false);
                }
            }
        }

        return outputEntry;
    }

    private CodeBlock getOutputSchemaCodeBlock(String mimeType, MediaType mediaType) {
        CodeBlock.Builder builder = CodeBlock.builder();

        Schema<?> schema = mediaType.getSchema();

        builder.add(getSchemaCodeBlock(null, schema.getDescription(), null, null, schema, true, true, openAPI, false));

        builder.add(
            """
                .metadata(
                   $T.of(
                     "responseType", ResponseType.$L
                   )
                )
                """,
            Map.class,
            getResponseType(mimeType, schema));

        return builder.build();
    }

    private static String getResponseType(String mimeType, Schema<?> schema) {
        String responseType;

        if (Objects.equals(schema.getType(), "string") && Objects.equals(schema.getFormat(), "binary")) {
            responseType = (mimeType == null) ? "BINARY" : "binary(\"" + mimeType + "\")";
        } else {
            responseType = switch (mimeType) {
                case "application/json" -> "JSON";
                case "application/xml" -> "XML";
                case "application/octet-stream" -> "BINARY";
                default -> "TEXT";
            };
        }

        return responseType;
    }

    private String getPackageName() {
        return deleteWhitespace(StringUtils.isEmpty(basePackageName) ? "" : basePackageName + ".") +
            StringUtils.replaceChars(componentName, "-_", ".");
    }

    private CodeBlock getParametersPropertiesCodeBlock(Operation operation, OpenAPI openAPI) {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        List<Parameter> parameters = operation.getParameters();

        if (parameters != null) {
            for (Parameter parameter : parameters) {
                CodeBlock.Builder builder = CodeBlock.builder();

                builder.add(
                    getSchemaCodeBlock(
                        parameter.getName(), parameter.getDescription(), parameter.getRequired(), null,
                        parameter.getSchema(), false, false, openAPI, false));
                builder.add(
                    CodeBlock.of(
                        """
                            .metadata(
                               $T.of(
                                 "type", PropertyType.$L
                               )
                            )
                            """,
                        Map.class,
                        StringUtils.upperCase(parameter.getIn())));

                codeBlocks.add(builder.build());
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
    }

    private ClassName getPropertiesClassName(String schemaName) {
        return ClassName.get(
            getPackageName() + ".property", getComponentClassName(componentName) + schemaName + "Properties");
    }

    private PropertiesEntry getPropertiesEntry(Operation operation, OpenAPI openAPI) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        CodeBlock codeBlock = getParametersPropertiesCodeBlock(operation, openAPI);

        if (!codeBlock.isEmpty()) {
            codeBlocks.add(codeBlock);
        }

        PropertiesEntry requestBodyPropertiesEntry = getRequestBodyPropertiesItem(operation, openAPI);

        if (requestBodyPropertiesEntry != null) {
            codeBlock = requestBodyPropertiesEntry.propertiesCodeBlock();

            if (!codeBlock.isEmpty()) {
                codeBlocks.add(codeBlock);
            }
        }

        codeBlock = codeBlocks.stream()
            .collect(CodeBlock.joining(","));

        return new PropertiesEntry(codeBlock, requestBodyPropertiesEntry);
    }

    private CodeBlock getPropertiesCodeBlock(
        String propertyName, String schemaName, Schema<?> schema, boolean outputSchema, OpenAPI openAPI) {

        CodeBlock.Builder builder = CodeBlock.builder();
        CodeBlock propertiesCodeBlock;

        if (schemas.contains(schemaName)) {
            propertiesCodeBlock = CodeBlock.of("$T.PROPERTIES", getPropertiesClassName(schemaName));
        } else {
            propertiesCodeBlock = getObjectPropertiesCodeBlock(propertyName, schema, outputSchema, openAPI);
        }

        builder.add(".properties($L)", propertiesCodeBlock);

        return builder.build();
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private CodeBlock getPropertiesSchemaCodeBlock(
        Map<String, Schema> properties, List<String> required, boolean outputSchema, OpenAPI openAPI) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            CodeBlock codeBlock;
            Schema schema = entry.getValue();

            if (schema.getAllOf() == null) {
                codeBlock = getSchemaCodeBlock(
                    entry.getKey(), schema.getDescription(), required.contains(entry.getKey()), null, schema, false,
                    outputSchema, openAPI, false);
            } else {
                codeBlock = getAllOfSchemaCodeBlock(
                    entry.getKey(), schema.getDescription(), schema.getAllOf(), outputSchema, openAPI);
            }

            if (codeBlock.isEmpty()) {
                throw new IllegalArgumentException("Schema is not supported: %s".formatted(schema));
            } else {
                codeBlocks.add(codeBlock);
            }
        }

        return codeBlocks
            .stream()
            .collect(CodeBlock.joining(","));
    }

    @SuppressWarnings("rawtypes")
    private CodeBlock getRefCodeBlock(
        String propertyName, Boolean required, Schema<?> schema, boolean excludePropertyNameIfEmpty,
        boolean outputSchema, OpenAPI openAPI, boolean bodySchema) {

        String ref = schema.get$ref();
        Components components = openAPI.getComponents();

        Map<String, Schema> schemaMap = components.getSchemas();

        String curSchemaName = ref.replace("#/components/schemas/", "");

        schemas.add(curSchemaName);

        schema = schemaMap.get(curSchemaName);

        return getSchemaCodeBlock(
            propertyName, schema.getDescription(), required, curSchemaName, schema, excludePropertyNameIfEmpty,
            outputSchema, openAPI, bodySchema);
    }

    @SuppressWarnings({
        "rawtypes"
    })
    private PropertiesEntry getRequestBodyPropertiesItem(Operation operation, OpenAPI openAPI) {
        CodeBlock.Builder builder = CodeBlock.builder();
        RequestBody requestBody = operation.getRequestBody();
        PropertiesEntry requestBodyPropertiesEntry = null;

        if (requestBody != null) {
            Content content = requestBody.getContent();

            String bodyContentType = null;
            String mimeType = null;
            Set<Map.Entry<String, MediaType>> entries = content.entrySet();

            if (!entries.isEmpty()) {
                mimeType = getMimeType(entries);

                // CHECKSTYLE:OFF
                bodyContentType = switch (mimeType) {
                    case "application/json" -> "JSON";
                    case "application/xml" -> "XML";
                    case "application/x-www-form-urlencoded" -> "FORM_URL_ENCODED";
                    case "application/octet-stream" -> "BINARY";
                    case "multipart/form-data" -> "FORM_DATA";
                    default -> "RAW";
                };
                // CHECKSTYLE:ON

                MediaType mediaType = content.get(mimeType);

                Schema schema = mediaType.getSchema();

                builder.add(
                    getSchemaCodeBlock(
                        null, null, requestBody.getRequired(), null, schema, false, false, openAPI, true));
            }

            requestBodyPropertiesEntry = new PropertiesEntry(builder.build(), bodyContentType, mimeType);
        }

        return requestBodyPropertiesEntry;
    }

    private ClassName getReturnTypeForOption(String type) {
        return switch (type) {
            case "integer" -> ClassName.get(Long.class);
            case "string" -> ClassName.get(String.class);
            default ->
                throw new IllegalArgumentException("Type %s is not supported for dynamic options.".formatted(type));
        };
    }

    private static CodeBlock getSampleOutputCodeBlock(Object sampleOutput) {
        CodeBlock.Builder builder = CodeBlock.builder();

        switch (sampleOutput) {
            case null -> {
                return builder.build();
            }
            case String string -> {
                Object convertedSampleOutput = null;
                JsonNode jsonNode = null;

                try {
                    jsonNode = OBJECT_MAPPER.readTree(string);
                } catch (JsonProcessingException e) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage(), e);
                    }
                }

                if (jsonNode != null) {
                    convertedSampleOutput = convert(jsonNode, new TypeReference<Map<String, Object>>() {});

                    if (convertedSampleOutput == null) {
                        convertedSampleOutput = convert(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
                    }
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Boolean>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Double>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Float>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Long>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Integer>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Short>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<LocalDateTime>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<LocalDate>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<Map<String, Object>>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<List<?>>() {});
                }

                if (convertedSampleOutput != null) {
                    sampleOutput = convertedSampleOutput;
                }
            }
            case ObjectNode objectNode -> {
                sampleOutput = convert(objectNode, new TypeReference<Map<String, Object>>() {});

                if (sampleOutput == null) {
                    sampleOutput = convert(objectNode, new TypeReference<List<Map<String, Object>>>() {});
                }
            }
            default -> {
            }
        }

        switch (sampleOutput) {
            case LocalDateTime localDateTime -> builder.add(
                "$T.of($L,$L,$L,$L,$L,$L)", ClassName.get(LocalDateTime.class),
                localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
            case LocalDate localDate -> builder.add(
                "$T.of($L,$L,$L)", ClassName.get(LocalDate.class), localDate.getYear(), localDate.getMonthValue(),
                localDate.getDayOfMonth());
            case Collection<?> collection -> {
                List<?> list = new ArrayList<>(collection);

                builder.add("$T.of(", ClassName.get(List.class));

                for (int i = 0; i < list.size(); i++) {
                    builder.add("$L", getSampleOutputCodeBlock(list.get(i)));

                    if (i < list.size() - 1) {
                        builder.add(",");
                    }
                }

                builder.add(")");
            }
            case Map<?, ?> map -> {
                builder.add("$T.<String, Object>ofEntries(", ClassName.get(Map.class));

                List<Map.Entry<String, CodeBlock>> entries = map.entrySet()
                    .stream()
                    .map(entry -> Map.entry((String) entry.getKey(), getSampleOutputCodeBlock(entry.getValue())))
                    .toList();

                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<String, CodeBlock> entry = entries.get(i);

                    CodeBlock valueCodeBlock = entry.getValue();

                    if (valueCodeBlock.isEmpty()) {
                        builder.add("Map.entry($S,$S)", entry.getKey(), "");
                    } else {
                        builder.add("Map.entry($S,$L)", entry.getKey(), entry.getValue());
                    }

                    if (i < entries.size() - 1) {
                        builder.add(",");
                    }
                }

                builder.add(")");
            }
            case String string -> builder.add("$S", string);
            case null, default -> builder.add("$L", sampleOutput);
        }

        return builder.build();
    }

    @SuppressWarnings({
        "checkstyle:methodlength", "rawtypes"
    })
    private CodeBlock getSchemaCodeBlock(
        String propertyName, String propertyDescription, Boolean required, String schemaName, Schema<?> schema,
        boolean excludePropertyNameIfEmpty, boolean outputSchema, OpenAPI openAPI, boolean bodySchema) {

        CodeBlock.Builder builder = CodeBlock.builder();

        Map<String, Object> extensionMap = schema.getExtensions();

        if (extensionMap == null || extensionMap.containsKey("x-dynamic-options") ||
            !extensionMap.containsKey("x-dynamic-properties")) {

            if (StringUtils.isEmpty(schema.get$ref())) {
                String type = StringUtils.isEmpty(schema.getType()) ? "object" : schema.getType();

                switch (type) {
                    case "array" -> {
                        if (StringUtils.isEmpty(propertyName) && excludePropertyNameIfEmpty) {
                            builder.add(
                                "array().items($L)",
                                getSchemaCodeBlock(
                                    null, schema.getDescription(), null, null, schema.getItems(), true, outputSchema,
                                    openAPI, bodySchema));
                        } else {
                            propertyName = StringUtils.isEmpty(propertyName)
                                ? schema.getTitle() == null ? "__items" : buildPropertyNameFromTitle(schema.getTitle())
                                : propertyName;

                            builder.add(
                                "array($S).items($L)",
                                propertyName,
                                getSchemaCodeBlock(
                                    null, schema.getDescription(), null, null, schema.getItems(), true, outputSchema,
                                    openAPI, bodySchema));
                        }

                        if (!outputSchema) {
                            builder.add(
                                ".placeholder($S)", "Add to " + buildPropertyLabel(propertyName.replace("__", "")));
                        }

                        if (bodySchema) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    case "boolean" -> {
                        builder.add("bool($S)", propertyName);

                        if (bodySchema) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    case "integer" -> {
                        builder.add("integer($S)", propertyName);

                        if (schema.getMinimum() != null) {
                            BigDecimal minimum = schema.getMinimum();

                            builder.add(".minValue($L)", minimum.intValue());
                        }

                        if (schema.getMaximum() != null) {
                            BigDecimal maximum = schema.getMaximum();

                            builder.add(".maxValue($L)", maximum.intValue());
                        }

                        if (bodySchema) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    case "number" -> {
                        builder.add("number($S)", propertyName);

                        if (schema.getMinimum() != null) {
                            BigDecimal minimum = schema.getMinimum();

                            builder.add(".minValue($L)", minimum.doubleValue());
                        }

                        if (schema.getMaximum() != null) {
                            BigDecimal maximum = schema.getMaximum();

                            builder.add(".maxValue($L)", maximum.doubleValue());
                        }

                        if (bodySchema) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    case "object" -> {
                        if (StringUtils.isEmpty(propertyName) && excludePropertyNameIfEmpty) {
                            builder.add("object()");

                            if (schema.getProperties() != null || schema.getAllOf() != null) {
                                builder.add(
                                    getPropertiesCodeBlock(propertyName, schemaName, schema, outputSchema, openAPI));
                            }

                            if (schema.getAdditionalProperties() != null) {
                                builder.add(getAdditionalPropertiesCodeBlock(propertyName, schema, outputSchema));
                            }
                        } else {
                            if (StringUtils.isEmpty(propertyName)) {

                                if (schema.getProperties() != null || schema.getAllOf() != null) {
                                    List<CodeBlock> codeBlocks = new ArrayList<>();

                                    for (Map.Entry<String, Schema> entry : schema.getProperties()
                                        .entrySet()) {
                                        String propertyName1 = entry.getKey();
                                        Boolean required1 = schema.getRequired() != null && schema.getRequired()
                                            .contains(propertyName1);
                                        Schema<?> propertySchema = entry.getValue();

                                        codeBlocks.add(getSchemaCodeBlock(
                                            propertyName1, propertySchema.getDescription(), required1,
                                            schemaName, propertySchema, false, outputSchema, openAPI, bodySchema));
                                    }

                                    CodeBlock collect = codeBlocks.stream()
                                        .collect(CodeBlock.joining(","));
                                    builder.add(collect);
                                }

                            } else {
                                builder.add("object($S)", propertyName);

                                if (schema.getProperties() != null || schema.getAllOf() != null) {
                                    builder.add(
                                        getPropertiesCodeBlock(propertyName, schemaName, schema, outputSchema,
                                            openAPI));
                                }

                                if (schema.getAdditionalProperties() != null) {
                                    builder.add(getAdditionalPropertiesCodeBlock(propertyName, schema, outputSchema));
                                }
                            }
                        }

                        if (bodySchema && !StringUtils.isEmpty(propertyName)) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    case "string" -> {
                        switch (schema.getFormat()) {
                            case "date" -> builder.add("date($S)", propertyName);
                            case "date-time" -> builder.add("dateTime($S)", propertyName);
                            case "binary" -> {
                                if (outputSchema) {
                                    builder.add("fileEntry()");
                                } else {
                                    builder.add("fileEntry($S)",
                                        StringUtils.isEmpty(propertyName) ? "fileEntry" : propertyName);
                                }
                            }
                            case null, default -> {
                                if (StringUtils.isEmpty(propertyName)) {
                                    builder.add("string()");
                                } else {
                                    builder.add("string($S)", propertyName);
                                }
                            }
                        }

                        if (schema.getMinLength() != null) {
                            Integer minLength = schema.getMinLength();

                            builder.add(".minLength($L)", minLength);
                        }

                        if (schema.getMaxLength() != null) {
                            Integer maxLength = schema.getMaxLength();

                            builder.add(".maxLength($L)", maxLength);
                        }

                        if (bodySchema) {
                            builder.add(
                                """
                                    .metadata(
                                       $T.of(
                                         "type", PropertyType.BODY
                                       )
                                    )
                                    """,
                                Map.class);
                        }
                    }
                    default -> throw new IllegalArgumentException(
                        "Parameter type %s is not supported.".formatted(schema.getType()));
                }

                checkAdditionalProperties(
                    propertyName, propertyDescription, required, schema, outputSchema, type, builder);
            } else {
                builder.add(
                    getRefCodeBlock(
                        propertyName, required, schema, excludePropertyNameIfEmpty, outputSchema, openAPI, bodySchema));
            }

            handleDynamicOptions(propertyName, schema, extensionMap, builder);

        } else if (extensionMap.containsKey("x-dynamic-properties")) {
            builder.add(addDynamicProperty(propertyName, propertyDescription, required, extensionMap, bodySchema));
        }

        return builder.build();
    }

    private void handleDynamicOptions(
        String propertyName, Schema<?> schema, Map<String, Object> extensionMap, CodeBlock.Builder builder) {

        if (extensionMap != null && Boolean.TRUE.equals(extensionMap.get("x-dynamic-options"))) {
            String type = StringUtils.isEmpty(schema.getType()) ? "object" : schema.getType();
            String optionType;

            switch (type) {
                case "integer" -> optionType = "Long";
                case "string" -> optionType = "String";
                case "array" -> {
                    String itemsType = schema.getItems()
                        .getType();

                    if (Objects.equals(itemsType, "integer")) {
                        optionType = "Long";
                    } else if (Objects.equals(itemsType, "string")) {
                        optionType = "String";
                    } else {
                        throw new IllegalArgumentException(
                            "Parameter type %s is not supported yet.".formatted(itemsType));
                    }

                    type = itemsType;
                }
                case null, default ->
                    throw new IllegalArgumentException("Parameter type %s is not supported yet.".formatted(type));
            }

            dynamicOptionsMap.put(buildOptionsFunctionsName(propertyName), type);

            builder.add(
                ".options(($T.OptionsFunction<" + optionType + ">)$T::get"
                    + buildOptionsFunctionsName(propertyName) + "Options)",
                ClassName.get("com.bytechef.component.definition", "ActionDefinition"),
                ClassName.get("com.bytechef.component." + componentName + ".util",
                    getComponentClassName(componentName) + "Utils"));

            if (extensionMap.get("x-dynamic-options-dependency") instanceof List<?> dependencies) {
                List<String> allDependencies = dependencies.stream()
                    .map(object -> "\"" + object + "\"")
                    .toList();

                builder.add(".optionsLookupDependsOn($L)", String.join(",", allDependencies));
            }
        }
    }

    private Path writeAbstractComponentHandlerSource(Path sourceDirPath) throws IOException {
        JavaFile javaFile = addStaticImport(
            JavaFile.builder(
                getPackageName(),
                TypeSpec.classBuilder("Abstract" + getComponentHandlerClassName(componentName))
                    .addJavadoc("""
                        Provides the base implementation for the REST based component.

                        @generated
                        """)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addSuperinterface(
                        ClassName.get(COM_BYTECHEF_COMPONENT_PACKAGE, "OpenApiComponentHandler"))
                    .addField(FieldSpec.builder(COMPONENT_DEFINITION_CLASS_NAME, "componentDefinition")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer(getComponentCodeBlock(sourceDirPath))
                        .build())
                    .addMethod(MethodSpec.methodBuilder("getDefinition")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(COMPONENT_DEFINITION_CLASS_NAME)
                        .addStatement("return componentDefinition")
                        .build())
                    .build()))
                        .build();

        return javaFile.writeToPath(sourceDirPath);
    }

    private void writeAbstractUtils(Path sourceDirPath) throws IOException {
        TypeSpec.Builder builder = TypeSpec.classBuilder("Abstract" + getComponentClassName(componentName) + "Utils")
            .addJavadoc(
                """
                    Provides methods for retrieving dynamic options and properties for various properties within the component.

                    @generated
                    """)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        dynamicOptionsMap.forEach((propertyName, type) -> {
            ClassName returnType = getReturnTypeForOption(type);
            builder.addMethod(createOptionsMethod(propertyName, returnType));
        });

        dynamicProperties.forEach(s -> builder.addMethod(createPropertiesMethod(s)));

        JavaFile javaFile = addStaticImport(
            JavaFile.builder(
                getPackageName() + ".util",
                builder.build()))
                    .build();

        javaFile.writeToPath(sourceDirPath);
    }

    private void writeAbstractComponentHandlerTest(Path testDirPath) throws IOException {
        String componentHandlerClassName = getComponentHandlerClassName(componentName);

        JavaFile javaFile = JavaFile.builder(
            getPackageName(),
            TypeSpec.classBuilder("Abstract" + componentHandlerClassName + "Test")
                .addJavadoc("""
                    Provides the base test implementation for the REST based component.

                    @generated
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addMethod(MethodSpec.methodBuilder("testGetDefinition")
                    .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement(
                        "$T.assertEquals(\"definition/" + componentName + "_v1.json\", new $T().getDefinition())",
                        ClassName.get("com.bytechef.test.jsonasssert", "JsonFileAssert"),
                        ClassName.get(getPackageName(), componentHandlerClassName))
                    .build())
                .build())
            .build();

        javaFile.writeToPath(testDirPath);
    }

    private void writeComponentActionSource(
        ClassName className, CodeBlock actionsCodeBlock, Path componentHandlerDirPath) throws IOException {

        JavaFile javaFile = addStaticImport(JavaFile.builder(
            className.packageName(),
            TypeSpec.classBuilder(className.simpleName())
                .addJavadoc("""
                    Provides a list of the component actions.

                    @generated
                    """)
                .addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(
                    ClassName.get(
                        "com.bytechef.component.definition",
                        "ComponentDsl",
                        "ModifiableActionDefinition"),
                    "ACTION_DEFINITION")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(actionsCodeBlock)
                    .build())
                .build()))
                    .build();

        javaFile.writeTo(componentHandlerDirPath);
    }

    private void writeComponentConnectionSource(
        ClassName className, CodeBlock connectionCodeBlock, Path componentHandlerDirPath) throws IOException {

        JavaFile javaFile = addStaticImport(
            JavaFile.builder(
                className.packageName(),
                TypeSpec.classBuilder(className.simpleName())
                    .addJavadoc("""
                        Provides the component connection definition.

                        @generated
                        """)
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .addField(FieldSpec.builder(
                        ClassName.get(
                            "com.bytechef.component.definition",
                            "ComponentDsl",
                            "ModifiableConnectionDefinition"),
                        "CONNECTION_DEFINITION")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(connectionCodeBlock)
                        .build())
                    .build()))
                        .build();

        javaFile.writeTo(componentHandlerDirPath);
    }

    private void writeComponentHandlerDefinition(
        Path componentHandlerSourcePath, String packageName, int version) throws Exception {

        String resourcesPathname = getAbsolutePathname("src" + File.separator + "test" + File.separator + "resources");

        Path definitionDirPath = Files.createDirectories(Paths.get(resourcesPathname + File.separator + "definition"));

        Path defintionFilePath = definitionDirPath.resolve(componentName + "_v" + version + ".json");

        File definitionFile = defintionFilePath.toFile();

        if (!definitionFile.exists()) {
            Path classPath = compileComponentHandlerSource(componentHandlerSourcePath);

            OpenApiComponentHandler openApiComponentHandler = createComponentHandler(
                classPath, packageName + "." + getComponentHandlerClassName(componentName));

            OBJECT_MAPPER.writeValue(definitionFile, openApiComponentHandler.getDefinition());
        }
    }

    private void writeComponentHandlerSource(Path sourceDirPath) throws IOException {
        String packageName = getPackageName();

        String filename = sourceDirPath
            .resolve(packageName.replace(".", File.separator))
            .resolve(getComponentHandlerClassName(componentName) + ".java")
            .toFile()
            .getAbsolutePath();

        if (!new File(filename).exists()) {
            JavaFile javaFile = JavaFile.builder(
                getPackageName(),
                TypeSpec.classBuilder(getComponentHandlerClassName(componentName))
                    .addJavadoc("This class will not be overwritten on the subsequent calls of the generator.")
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName.get("com.google.auto.service", "AutoService"))
                            .addMember("value", "$T.class",
                                ClassName.get("com.bytechef.component", "OpenApiComponentHandler"))
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(
                        ClassName.get(getPackageName(), "Abstract" + getComponentHandlerClassName(componentName)))
                    .build())
                .build();

            javaFile.writeToPath(sourceDirPath);
        }
    }

    private void writeComponentHandlerTest(Path testDirPath) throws IOException {
        String packageName = getPackageName();

        String componentHandlerTestClassname = getComponentHandlerClassName(componentName) + "Test";

        String filename = testDirPath
            .resolve(packageName.replace(".", File.separator))
            .resolve(componentHandlerTestClassname + ".java")
            .toFile()
            .getAbsolutePath();

        if (!new File(filename).exists()) {
            JavaFile javaFile = JavaFile.builder(
                getPackageName(),
                TypeSpec.classBuilder(componentHandlerTestClassname)
                    .addJavadoc("@generated")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ClassName.get(getPackageName(), "Abstract" + componentHandlerTestClassname))
                    .build())
                .build();

            javaFile.writeToPath(testDirPath);
        }
    }

    private void writeComponentSchemaSource(
        ClassName className, CodeBlock componentSchemaCodeBlock, Path componentHandlerDirPath) throws IOException {

        JavaFile javaFile = addStaticImport(
            JavaFile.builder(
                className.packageName(),
                TypeSpec.classBuilder(className.simpleName())
                    .addJavadoc("""
                        Provides properties definition built from OpenAPI schema.

                        @generated
                        """)
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .addField(FieldSpec.builder(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"),
                            ParameterizedTypeName.get(
                                ClassName.get(
                                    "com.bytechef.component.definition", "ComponentDsl", "ModifiableValueProperty"),
                                WildcardTypeName.subtypeOf(Object.class), WildcardTypeName.subtypeOf(Object.class))),
                        "PROPERTIES")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.of($L)", List.class, componentSchemaCodeBlock)
                        .build())
                    .build()))
                        .build();

        javaFile.writeTo(componentHandlerDirPath);
    }

    @SuppressWarnings("rawtypes")
    private void writeComponentSchemaSources(Set<String> schemas, Path sourceDirPath) throws IOException {
        Components components = openAPI.getComponents();

        if (components != null) {
            Map<String, Schema> schemaMap = components.getSchemas();

            if (schemaMap != null) {
                for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
                    if (!schemas.contains(entry.getKey())) {
                        continue;
                    }

                    Schema schema = entry.getValue();

                    if (schema.getType() != null && !Objects.equals(schema.getType(), "object")) {
                        continue;
                    }

                    ClassName className = getPropertiesClassName(entry.getKey());

                    writeComponentSchemaSource(
                        className, getObjectPropertiesCodeBlock(null, schema, false, openAPI), sourceDirPath);
                }
            }
        }
    }

    private void writeUtils(Path sourceDirPath) throws IOException {
        String packageName = getPackageName() + ".util";

        String filename = sourceDirPath
            .resolve(packageName.replace(".", File.separator))
            .resolve(getComponentClassName(componentName) + "Utils.java")
            .toFile()
            .getAbsolutePath();

        if (!new File(filename).exists()) {
            JavaFile javaFile = JavaFile.builder(
                getPackageName() + ".util",
                TypeSpec.classBuilder(getComponentClassName(componentName) + "Utils")
                    .addJavadoc("This class will not be overwritten on the subsequent calls of the generator.")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(
                        ClassName.get(getPackageName() + ".util",
                            "Abstract" + getComponentClassName(componentName) + "Utils"))
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build())
                    .build())
                .build();

            javaFile.writeToPath(sourceDirPath);
        }
    }

    private record OperationItem(Operation operation, String method, String path) {
        public String getOperationId() {
            return operation.getOperationId();
        }
    }

    private record OutputEntry(CodeBlock outputSchemaCodeBlock, CodeBlock sampleOutputCodeBlock, boolean isDynamic) {
    }

    private record PropertiesEntry(CodeBlock propertiesCodeBlock, String bodyContentType, String mimeType) {
        public PropertiesEntry(CodeBlock codeBlock, PropertiesEntry propertiesEntry) {
            this(
                codeBlock, propertiesEntry == null ? null : propertiesEntry.bodyContentType,
                propertiesEntry == null ? null : propertiesEntry.mimeType);
        }
    }

    private static class GeneratorConfig {
        @JsonProperty
        private OpenApi openApi = new OpenApi();

        private static class OpenApi {
            @JsonProperty
            private List<String> operations = Collections.emptyList();

            @JsonProperty
            private List<String> oAuth2Scopes = Collections.emptyList();
        }
    }
}

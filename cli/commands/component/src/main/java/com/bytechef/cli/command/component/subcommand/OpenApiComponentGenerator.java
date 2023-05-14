
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

import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiComponentGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiComponentGenerator.class);

    private static final ClassName AUTHORIZATION_CLASS_NAME = ClassName.get(
        "com.bytechef.hermes.component.definition", "Authorization");
    public static final String COM_BYTECHEF_HERMES_COMPONENT_PACKAGE = "com.bytechef.hermes.component";
    public static final ClassName COMPONENT_DEFINITION_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_HERMES_COMPONENT_PACKAGE + ".definition", "ComponentDefinition");
    public static final ClassName CONNECTION_DEFINITION_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_HERMES_COMPONENT_PACKAGE + ".definition", "ConnectionDefinition");
    public static final ClassName COMPONENT_DSL_CLASS_NAME = ClassName
        .get(COM_BYTECHEF_HERMES_COMPONENT_PACKAGE + ".definition", "ComponentDSL");
    private static final ClassName HTTP_CLIENT_UTILS_CLASS = ClassName
        .get("com.bytechef.hermes.component.util", "HttpClientUtils");
    private static final ClassName OPEN_API_COMPONENT_HANDLER_CLASS = ClassName
        .get("com.bytechef.hermes.component", "OpenApiComponentHandler");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            enable(SerializationFeature.INDENT_OUTPUT);
            registerModule(new Jdk8Module());
        }
    };
    private static final String[] COMPONENT_DIR_NAMES = {
        "action", "connection", "property", "trigger", "util"
    };

    private final String basePackageName;
    private final String componentName;
    private final GeneratorConfig generatorConfig;
    private final OpenAPI openAPI;
    private final String outputPath;
    private final Set<String> schemas = new HashSet<>();
    private final boolean internalComponent;
    private final int version;
    private final Set<String> oAuth2Scopes = new HashSet<>();

    public OpenApiComponentGenerator(
        String basePackageName, String componentName, int version, boolean internalComponent, String openApiPath,
        String outputPath) throws IOException {

        this.basePackageName = basePackageName;
        this.componentName = componentName;
        this.version = version;
        this.internalComponent = internalComponent;
        this.openAPI = parseOpenAPIFile(openApiPath);
        this.outputPath = outputPath;

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

        if (internalComponent) {
            Path sourceTestJavaDirPath = Files.createDirectories(
                Paths.get(getAbsolutePathname("src" + File.separator + "test" + File.separator + "java")));

            writeAbstractComponentHandlerTest(sourceTestJavaDirPath);
            writeComponentHandlerDefinition(componentHandlerSourcePath, getPackageName(), version);
            writeComponentHandlerTest(sourceTestJavaDirPath);
        }
    }

    private JavaFile.Builder addStaticImport(JavaFile.Builder builder) {
        return builder.addStaticImport(AUTHORIZATION_CLASS_NAME, "ADD_TO")
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
            .addStaticImport(AUTHORIZATION_CLASS_NAME, "ApiTokenLocation", "AuthorizationType")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "authorization")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "component")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "connection")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "array")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "action")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "bool")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "date")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "dateTime")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "fileEntry")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "integer")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "number")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "object")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "option")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "oneOf")
            .addStaticImport(COMPONENT_DSL_CLASS_NAME, "string")
            .addStaticImport(HTTP_CLIENT_UTILS_CLASS, "BodyContentType", "ResponseFormat")
            .addStaticImport(OPEN_API_COMPONENT_HANDLER_CLASS, "PropertyType")
            .addStaticImport(CONNECTION_DEFINITION_CLASS_NAME, "BASE_URI");
    }

    private static String buildPropertyName(String propertyName) {
        return Arrays.stream(StringUtils.split(propertyName, '_'))
            .flatMap(item -> Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(item)))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
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

                    getObjectPropertiesCodeBlock(null, entry.getValue(), openAPI);
                }
            }
        }
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

    private Path compileComponentHandlerSource(Path sourcePath) {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        List<String> javacOpts = new ArrayList<>();

        javacOpts.add("-classpath");
        javacOpts.add(
            "libs/auto-service-annotations-1.0.1.jar:libs/hermes-component-api-1.0.jar:" +
                "libs/hermes-definition-api-1.0.jar");

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
            tempDirPath = Files.createTempDirectory("rest_component");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        javacOpts.add("-d");
        javacOpts.add(tempDirPath.toString());

        String simpleClassName = getComponentHandlerClassName(componentName);

        javacOpts.add(sourcePath.getParent() + "/Abstract" + simpleClassName + ".java");
        javacOpts.add(sourcePath.getParent() + "/" + simpleClassName + ".java");
        javacOpts.add(
            sourcePath.getParent() + "/connection/" + StringUtils.capitalize(componentName) + "Connection.java");

        javaCompiler.run(null, null, null, javacOpts.toArray(new String[0]));

        return tempDirPath;
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
        return outputPath + File.separator + componentName + File.separator + subPath;
    }

    private CodeBlock getActionCodeBlock(OperationItem operationItem, OpenAPI openAPI) {
        Operation operation = operationItem.operation();
        String method = operationItem.method();

        OutputEntry outputEntry = getOutputEntry(operation, openAPI);
        PropertiesEntry propertiesEntry = getPropertiesEntry(operation, openAPI);

        CodeBlock.Builder builder = CodeBlock.builder();

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

        CodeBlock codeBlock = outputEntry == null ? null : outputEntry.outputCodeBlock();

        if (codeBlock != null && !codeBlock.isEmpty()) {
            builder.add(".outputSchema($L)", codeBlock);
        }

        Object sampleOutput = outputEntry == null ? null : outputEntry.sampleOutput();

        if (sampleOutput != null) {
            builder.add(".sampleOutput($S)", sampleOutput);
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
                    StringUtils.capitalize(componentName) + StringUtils.capitalize(operationItem.getOperationId()) +
                        "Action");

                writeComponentActionSource(className, actionCodeBlock, componentHandlerDirPath);

                codeBlocks.add(CodeBlock.of("$T.ACTION_DEFINITION", className));

                if (generatorConfig.openApi.oAuth2Scopes.isEmpty() && operationItem.operation.getSecurity() != null) {
                    collectOAuthScopes(operationItem.operation.getSecurity());
                }
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
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

        builder.add(
            """
                authorization(
                    AuthorizationType.API_KEY.toLowerCase(), AuthorizationType.API_KEY)
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
            "Value",
            true,
            addToCodeBlock);

        return builder.build();
    }

    private CodeBlock getAuthorizationBasicCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(
                    AuthorizationType.BASIC_AUTH.toLowerCase(), AuthorizationType.BASIC_AUTH)
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
                authorization(
                    AuthorizationType.BEARER_TOKEN.toLowerCase(), AuthorizationType.BEARER_TOKEN)
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
        String oAuth2Scopes = getOAUth2Scopes(oAuthFlow.getScopes());

        builder.add(
            """
                authorization(
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .authorizationUrl(connection -> $S)
                    $L.tokenUrl(connection -> $S)
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
                : CodeBlock.of(".scopes(connection -> $T.of($L))", List.class, getOAUth2Scopes(oAuthFlow.getScopes())),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl(connection -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2ClientCredentialsCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(
                    AuthorizationType.OAUTH2_CLIENT_CREDENTIALS.toLowerCase(), AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .scopes(connection -> $T.of($L))
                    .tokenUrl(connection -> $S)
                """,
            "Client Credentials",
            "Client Id",
            true,
            "OAuth2 Client Secret",
            true,
            List.class,
            getOAUth2Scopes(oAuthFlow.getScopes()),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl(connection -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2ImplicitCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(
                    AuthorizationType.OAUTH2_IMPLICIT_CODE.toLowerCase(), AuthorizationType.OAUTH2_IMPLICIT_CODE)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .authorizationUrl(connection -> $S)
                    .scopes(connection -> $T.of($L))
                """,
            "OAuth2 Implicit",
            "Client Id",
            true,
            "Client Secret",
            true,
            oAuthFlow.getAuthorizationUrl(),
            List.class,
            getOAUth2Scopes(oAuthFlow.getScopes()));

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl(connection -> $S)", oAuthFlow.getRefreshUrl());
        }

        return builder.build();
    }

    private CodeBlock getAuthorizationOAuth2PasswordCodeBlock(OAuthFlow oAuthFlow) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                authorization(
                    AuthorizationType.OAUTH2_RESOURCE_OWNER_PASSWORD.toLowerCase(), AuthorizationType.OAUTH2_RESOURCE_OWNER_PASSWORD)
                    .title($S)
                    .properties(
                        string(CLIENT_ID)
                            .label($S)
                            .required($L),
                        string(CLIENT_SECRET)
                            .label($S)
                            .required($L)
                    )
                    .scopes(connection -> $T.of($L))
                    .tokenUrl(connection -> $S)
                """,
            "OAuth2 Resource Owner Password",
            "Client Id",
            true,
            "Client Secret",
            true,
            List.class,
            oAuthFlow.getRefreshUrl(),
            getOAUth2Scopes(oAuthFlow.getScopes()),
            oAuthFlow.getTokenUrl());

        if (oAuthFlow.getRefreshUrl() != null) {
            builder.add(".refreshUrl(connection -> $S)", oAuthFlow.getRefreshUrl());
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
                        throw new IllegalStateException("Security scheme %s not supported".formatted(scheme));
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
                    throw new IllegalStateException(
                        "Security scheme type %s not supported: ".formatted(securityScheme.getType()));
                }
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
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

    private static CodeBlock getBaseUriCodeBlock(List<Server> servers) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (servers == null || servers.isEmpty()) {
            builder.add("null");
        } else {
            if (servers.size() == 1) {
                Server server = servers.get(0);

                if (!StringUtils.isEmpty(server.getUrl()) && !Objects.equals(server.getUrl(), "/")) {
                    builder.add(".baseUri(connection -> $S)", server.getUrl());
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

                Server server = servers.get(0);

                builder.add(".defaultValue($S)", server.getUrl());
                builder.add(")");
            }
        }

        return builder.build();
    }

    private CodeBlock getComponentCodeBlock(Path componentHandlerDirPath) throws IOException {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add(
            """
                modifyComponent(
                    component($S)
                        .title($S)
                        .description($S)
                    )
                    .actions(modifyActions($L))
                """,
            componentName,
            StringUtils.capitalize(componentName),
            openAPI.getInfo()
                .getDescription(),
            getActionsCodeBlock(componentHandlerDirPath, openAPI));

        CodeBlock codeBlock = getConnectionCodeBlock(openAPI, componentHandlerDirPath);

        if (!codeBlock.isEmpty()) {
            builder.add(".connection(modifyConnection($L))", codeBlock);
        }

        builder.add(".triggers(getTriggers())");

        return builder.build();
    }

    private String getComponentHandlerClassName(String componentName) {
        return StringUtils.capitalize(componentName) + "ComponentHandler";
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
                    getPackageName() + ".connection", StringUtils.capitalize(componentName) + "Connection");

                writeComponentConnectionSource(className, connectionCodeBlock, componentHandlerDirPath);

                builder.add(CodeBlock.of("$T.CONNECTION_DEFINITION", className));
            }
        }

        return builder.build();
    }

    private CodeBlock getObjectPropertiesCodeBlock(String name, Schema<?> schema, OpenAPI openAPI) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        if (schema.getProperties() != null) {
            codeBlocks.add(getPropertiesSchemaCodeBlock(
                schema.getProperties(), schema.getRequired() == null ? List.of() : schema.getRequired(), openAPI));
        }

        if (schema.getAllOf() != null) {
            codeBlocks.add(getAllOfSchemaCodeBlock(name, schema.getDescription(), schema.getAllOf(), openAPI));
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
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

    private String getOAUth2Scopes(Scopes scopes) {
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

    private Map<String, List<OperationItem>> getOperationItemsMap(io.swagger.v3.oas.models.Paths paths) {
        Map<String, List<OperationItem>> operationItemsMap = new LinkedHashMap<>();

        List<OperationItem> operationItems = new ArrayList<>();

        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            if (pathItem.getDelete() != null) {
                operationItems.add(new OperationItem(pathItem.getDelete(), "DELETE", path));
            }

            if (pathItem.getHead() != null) {
                operationItems.add(new OperationItem(pathItem.getHead(), "HEAD", path));
            }

            if (pathItem.getGet() != null) {
                operationItems.add(new OperationItem(pathItem.getGet(), "GET", path));
            }

            if (pathItem.getPatch() != null) {
                operationItems.add(new OperationItem(pathItem.getPatch(), "PATCH", path));
            }

            if (pathItem.getPost() != null) {
                operationItems.add(new OperationItem(pathItem.getPost(), "POST", path));
            }

            if (pathItem.getPut() != null) {
                operationItems.add(new OperationItem(pathItem.getPut(), "PUT", path));
            }
        }

        for (OperationItem operationItem : operationItems) {
            List<String> tags = operationItem.operation()
                .getTags();

            String tag = "unnamed";

            if (tags != null && !tags.isEmpty()) {
                tag = tags.get(0);
            }

            operationItemsMap.compute(tag, (key, tagOperationItems) -> {
                if (tagOperationItems == null) {
                    tagOperationItems = new ArrayList<>();
                }

                if (tagOperationItems.stream()
                    .noneMatch(curOperationItem -> Objects.equals(operationItem.getOperationId(),
                        curOperationItem.getOperationId()))) {
                    tagOperationItems.add(operationItem);
                }

                return tagOperationItems;
            });
        }

        return operationItemsMap;
    }

    @SuppressWarnings({
        "rawtypes"
    })
    private OutputEntry getOutputEntry(Operation operation, OpenAPI openAPI) {
        ApiResponse apiResponse = null;
        ApiResponses apiResponses = operation.getResponses();
        CodeBlock.Builder builder = CodeBlock.builder();
        OutputEntry outputEntry = null;

        for (String responseCode : List.of("200", "201", "default")) {
            apiResponse = apiResponses.get(responseCode);

            if (apiResponse != null) {
                break;
            }
        }

        if (apiResponse != null && apiResponse.getContent() != null) {
            Content content = apiResponse.getContent();

            Set<Map.Entry<String, MediaType>> entries = content.entrySet();

            if (!entries.isEmpty()) {
                String mimeType = getMimeType(entries);

                MediaType mediaType = content.get(mimeType);

                Schema schema = mediaType.getSchema();

                builder.add(getSchemaCodeBlock(null, schema.getDescription(), null, null, schema, openAPI, true));

                String responseFormat;

                if (Objects.equals(schema.getType(), "string") && Objects.equals(schema.getFormat(), "binary")) {
                    responseFormat = "BINARY";
                } else {
                    responseFormat = switch (mimeType) {
                        case "application/json" -> "JSON";
                        case "application/xml" -> "XML";
                        case "application/octet-stream" -> "BINARY";
                        default -> "TEXT";
                    };
                }

                builder.add(
                    """
                        .metadata(
                           $T.of(
                             "responseFormat", ResponseFormat.$L
                           )
                        )
                        """,
                    Map.class,
                    responseFormat);

                outputEntry = new OutputEntry(builder.build(), mediaType.getExample());
            }
        }

        return outputEntry;
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
                        parameter.getSchema(), openAPI, false));
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
        String propertyName, String schemaName, Schema<?> schema, OpenAPI openAPI) {

        CodeBlock.Builder builder = CodeBlock.builder();
        CodeBlock propertiesCodeBlock;

        if (schemas.contains(schemaName)) {
            propertiesCodeBlock = CodeBlock.of("$T.PROPERTIES", getPropertiesClassName(schemaName));
        } else {
            propertiesCodeBlock = getObjectPropertiesCodeBlock(propertyName, schema, openAPI);
        }

        builder.add(".properties($L)", propertiesCodeBlock);

        return builder.build();
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
                    case "application/x-www-form-urlencoded" -> "FORM_URLENCODED";
                    case "application/octet-stream" -> "BINARY";
                    case "multipart/form-data" -> "FORM_DATA";
                    default -> "RAW";
                };
                // CHECKSTYLE:ON

                MediaType mediaType = content.get(mimeType);

                Schema schema = mediaType.getSchema();

                builder.add(getSchemaCodeBlock(null, null, requestBody.getRequired(), null, schema, openAPI, false));
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

            requestBodyPropertiesEntry = new PropertiesEntry(builder.build(), bodyContentType, mimeType);
        }

        return requestBodyPropertiesEntry;
    }

    private CodeBlock getAdditionalPropertiesCodeBlock(Schema<?> schema, boolean outputEntry) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (schema.getAdditionalProperties() instanceof Boolean) {
            builder.add(".additionalProperties(oneOf())");
        } else {
            Schema<?> additionalPropertiesSchema = (Schema<?>) schema.getAdditionalProperties();

            if (StringUtils.isEmpty(additionalPropertiesSchema.get$ref())) {
                builder.add(".additionalProperties($L())", getAdditionalPropertiesItemType(additionalPropertiesSchema));
            } else {
                String ref = additionalPropertiesSchema.get$ref();

                String curSchemaName = ref.replace("#/components/schemas/", "");

                schemas.add(curSchemaName);

                builder.add(
                    ".additionalProperties(object().properties($L))",
                    CodeBlock.of("$T.PROPERTIES", getPropertiesClassName(curSchemaName)));
            }
        }

        if (!outputEntry) {
            builder.add(".placeholder($S)", "Add");
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
        String name, String description, List<Schema> allOfSchemas, OpenAPI openAPI) {

        Map<String, Schema> allOfProperties = getAllOfSchemaProperties(name, description, allOfSchemas);
        List<String> allOfRequired = new ArrayList<>();

        for (Schema allOfSchema : allOfProperties.values()) {
            if (allOfSchema.getRequired() != null) {
                allOfRequired.addAll(allOfSchema.getRequired());
            }
        }

        return getPropertiesSchemaCodeBlock(allOfProperties, allOfRequired, openAPI);
    }

    private ClassName getPropertiesClassName(String schemaName) {
        return ClassName.get(
            getPackageName() + ".property", StringUtils.capitalize(componentName) + schemaName + "Properties");
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private CodeBlock getPropertiesSchemaCodeBlock(
        Map<String, Schema> properties, List<String> required, OpenAPI openAPI) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            CodeBlock codeBlock;
            Schema schema = entry.getValue();

            if (schema.getAllOf() == null) {
                codeBlock = getSchemaCodeBlock(
                    entry.getKey(), schema.getDescription(), required.contains(entry.getKey()), null, schema, openAPI,
                    false);
            } else {
                codeBlock = getAllOfSchemaCodeBlock(entry.getKey(), schema.getDescription(), schema.getAllOf(),
                    openAPI);
            }

            if (codeBlock.isEmpty()) {
                throw new IllegalStateException("Schema is not supported: %s".formatted(schema));
            } else {
                codeBlocks.add(codeBlock);
            }
        }

        return codeBlocks.stream()
            .collect(CodeBlock.joining(","));
    }

    @SuppressWarnings({
        "rawtypes"
    })
    private CodeBlock getSchemaCodeBlock(
        String propertyName, String propertyDescription, Boolean required, String schemaName, Schema<?> schema,
        OpenAPI openAPI, boolean excludePropertyNameIfEmpty) {

        CodeBlock.Builder builder = CodeBlock.builder();

        if (StringUtils.isEmpty(schema.get$ref())) {
            String type = StringUtils.isEmpty(schema.getType()) ? "object" : schema.getType();

            switch (type) {
                case "array" -> {
                    if (StringUtils.isEmpty(propertyName) && excludePropertyNameIfEmpty) {
                        builder.add(
                            "array().items($L)",
                            getSchemaCodeBlock(
                                null, schema.getDescription(), null, null, schema.getItems(), openAPI, true));
                    } else {
                        builder.add(
                            "array($S).items($L)",
                            StringUtils.isEmpty(propertyName) ? "__items" : propertyName,
                            getSchemaCodeBlock(
                                null, schema.getDescription(), null, null, schema.getItems(), openAPI, true));
                    }

                    if (!excludePropertyNameIfEmpty) {
                        builder.add(".placeholder($S)", "Add");
                    }
                }
                case "boolean" -> builder.add("bool($S)", propertyName);
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
                }
                case "object" -> {
                    if (StringUtils.isEmpty(propertyName) && excludePropertyNameIfEmpty) {
                        builder.add("object()");
                    } else {
                        builder.add("object($S)", StringUtils.isEmpty(propertyName) ? "__item" : propertyName);
                    }

                    if (schema.getProperties() != null || schema.getAllOf() != null) {
                        builder.add(getPropertiesCodeBlock(propertyName, schemaName, schema, openAPI));
                    } else if (schema.getAdditionalProperties() != null) {
                        builder.add(getAdditionalPropertiesCodeBlock(schema, excludePropertyNameIfEmpty));
                    }
                }
                case "string" -> {
                    if (Objects.equals(schema.getFormat(), "date")) {
                        builder.add("date($S)", propertyName);
                    } else if (Objects.equals(schema.getFormat(), "date-time")) {
                        builder.add("dateTime($S)", propertyName);
                    } else if (Objects.equals(schema.getFormat(), "binary")) {
                        builder.add("fileEntry($S)", StringUtils.isEmpty(propertyName) ? "fileEntry" : propertyName);
                    } else {
                        if (StringUtils.isEmpty(propertyName)) {
                            builder.add("string()");
                        } else {
                            builder.add("string($S)", propertyName);
                        }
                    }
                }
                default -> throw new IllegalArgumentException(
                    "Parameter type %s is not supported.".formatted(schema.getType()));
            }

            if (propertyName != null) {
                propertyName = buildPropertyName(propertyName);

                builder.add(".label($S)", StringUtils.capitalize(propertyName));
            }

            if (propertyDescription != null) {
                builder.add(".description($S)", propertyDescription);
            }

            if (schema.getEnum() != null) {
                List<?> enums = schema.getEnum()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
                List<CodeBlock> codeBlocks = new ArrayList<>();

                for (Object item : enums) {
                    if (item instanceof String) {
                        codeBlocks.add(CodeBlock.of("option($S, $S)", StringUtils.capitalize(item.toString()), item));
                    } else {
                        codeBlocks.add(CodeBlock.of("option($S, $L)", StringUtils.capitalize(item.toString()), item));
                    }
                }

                if (!Objects.equals(type, "boolean")) {
                    builder.add(".options($L)", codeBlocks.stream()
                        .collect(CodeBlock.joining(",")));
                }
            }

            if (required != null) {
                builder.add(".required($L)", required);
            }

            if (schema.getExample() != null) {
                if (Objects.equals(type, "string")) {
                    builder.add(".exampleValue($S)", schema.getExample());
                } else {
                    builder.add(".exampleValue($L)", schema.getExample());
                }
            }
        } else {
            String ref = schema.get$ref();
            Components components = openAPI.getComponents();

            Map<String, Schema> schemaMap = components.getSchemas();

            String curSchemaName = ref.replace("#/components/schemas/", "");

            schemas.add(curSchemaName);

            schema = schemaMap.get(curSchemaName);

            builder.add(
                getSchemaCodeBlock(
                    StringUtils.isEmpty(propertyName) && !excludePropertyNameIfEmpty
                        ? StringUtils.uncapitalize(curSchemaName)
                        : propertyName,
                    schema.getDescription(), required, curSchemaName, schema, openAPI, excludePropertyNameIfEmpty));
        }

        return builder.build();
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

    private OpenApiComponentHandler runComponentHandlerClass(Path classPath, String className) throws Exception {
        File classFile = classPath.toFile();

        URI classURI = classFile.toURI();

        URL[] classUrls = new URL[] {
            classURI.toURL()
        };

        URLClassLoader classLoader = URLClassLoader.newInstance(classUrls);

        @SuppressWarnings("unchecked")
        Class<OpenApiComponentHandler> clazz = (Class<OpenApiComponentHandler>) Class.forName(className, true,
            classLoader);

        return clazz.getDeclaredConstructor()
            .newInstance();
    }

    private Path writeAbstractComponentHandlerSource(Path sourceDirPath) throws IOException {
        JavaFile javaFile = addStaticImport(JavaFile.builder(
            getPackageName(),
            TypeSpec.classBuilder("Abstract" + getComponentHandlerClassName(componentName))
                .addJavadoc("""
                    Provides the base implementation for the REST based component.

                    @generated
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(
                    ClassName.get(COM_BYTECHEF_HERMES_COMPONENT_PACKAGE, "OpenApiComponentHandler"))
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
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(
                    ClassName.get(
                        "com.bytechef.hermes.component.definition",
                        "ComponentDSL",
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

        JavaFile javaFile = addStaticImport(JavaFile.builder(
            className.packageName(),
            TypeSpec.classBuilder(className.simpleName())
                .addJavadoc("""
                    Provides the component connection definition.

                    @generated
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(
                    ClassName.get(
                        "com.bytechef.hermes.component.definition",
                        "ComponentDSL",
                        "ModifiableConnectionDefinition"),
                    "CONNECTION_DEFINITION")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(connectionCodeBlock)
                    .build())
                .build()))
                    .build();

        javaFile.writeTo(componentHandlerDirPath);
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
                    .addJavadoc("@generated")
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName.get("com.google.auto.service", "AutoService"))
                            .addMember("value", "$T.class",
                                ClassName.get("com.bytechef.hermes.component", "OpenApiComponentHandler"))
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ClassName.get(
                        getPackageName(),
                        "Abstract" + getComponentHandlerClassName(componentName)))
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
                    .superclass(ClassName.get(
                        getPackageName(),
                        "Abstract" + componentHandlerTestClassname))
                    .build())
                .build();
            javaFile.writeToPath(testDirPath);
        }
    }

    private void writeComponentHandlerDefinition(
        Path componentHandlerSourcePath, String packageName, int version) throws Exception {

        Path definitionDirPath = Files.createDirectories(
            Paths.get(
                getAbsolutePathname("src" + File.separator + "test" + File.separator + "resources") + File.separator +
                    "definition"));

        Path defintionFilePath = definitionDirPath.resolve(componentName + "_v" + version + ".json");

        File definitionFile = defintionFilePath.toFile();

        if (!definitionFile.exists()) {
            Path classPath = compileComponentHandlerSource(componentHandlerSourcePath);

            OpenApiComponentHandler openApiComponentHandler = runComponentHandlerClass(
                classPath, packageName + "." + getComponentHandlerClassName(componentName));

            OBJECT_MAPPER.writeValue(definitionFile, openApiComponentHandler.getDefinition());
        }
    }

    private void writeComponentSchemaSource(
        ClassName className, CodeBlock componentSchemaCodeBlock, Path componentHandlerDirPath) throws IOException {

        JavaFile javaFile = addStaticImport(JavaFile.builder(
            className.packageName(),
            TypeSpec.classBuilder(className.simpleName())
                .addJavadoc("""
                    Provides schema definition.

                    @generated
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "List"),
                        ClassName.get("com.bytechef.hermes.definition", "Property")),
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

                    if (!Objects.equals(schema.getType(), "object")) {
                        continue;
                    }

                    ClassName className = getPropertiesClassName(entry.getKey());

                    writeComponentSchemaSource(
                        className,
                        getObjectPropertiesCodeBlock(null, schema, openAPI),
                        sourceDirPath);
                }
            }
        }
    }

    private record OperationItem(Operation operation, String method, String path) {
        public String getOperationId() {
            return operation.getOperationId();
        }
    }

    private record OutputEntry(CodeBlock outputCodeBlock, Object sampleOutput) {
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

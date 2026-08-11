/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.generator;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property.ControlType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Builds the {@code ComponentDefinition} of an API connector directly from a parsed OpenAPI document, mirroring the
 * definition the CLI's {@code ComponentInitOpenApiGenerator} produces via generate-compile-classload — but without
 * generating, compiling, or class-loading any code. The serialized JSON stays compatible with what
 * {@code ComponentDefinitionReader} consumes; equivalence with the code-generating pipeline is pinned by
 * {@code OpenApiComponentDefinitionFactoryTest}, which compares both outputs on sample specifications.
 *
 * <p>
 * Codegen-only concerns are deliberately not carried over: the {@code x-dynamic-options}/{@code x-dynamic-properties}
 * extensions wired functions referencing generated stub classes that always returned empty lists, and function-valued
 * fields serialize as empty objects either way, so the extensions are ignored here and the affected properties are
 * built from their schema alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenApiComponentDefinitionFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            enable(SerializationFeature.INDENT_OUTPUT);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    private final String componentName;
    private final OpenAPI openAPI;
    private final Set<String> requiredScopes = new HashSet<>();

    private OpenApiComponentDefinitionFactory(String componentName, OpenAPI openAPI) {
        this.componentName = componentName;
        this.openAPI = openAPI;
    }

    public static ModifiableComponentDefinition createComponentDefinition(String componentName, OpenAPI openAPI) {
        return new OpenApiComponentDefinitionFactory(componentName, openAPI).createComponentDefinition();
    }

    public static String createComponentDefinitionJson(String componentName, OpenAPI openAPI) {
        try {
            return OBJECT_MAPPER.writeValueAsString(createComponentDefinition(componentName, openAPI));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize the component definition", exception);
        }
    }

    private ModifiableComponentDefinition createComponentDefinition() {
        String title = openAPI.getInfo() == null ? null
            : openAPI.getInfo()
                .getTitle();

        if (StringUtils.isEmpty(title)) {
            title = getComponentClassName(componentName);
        }

        List<OperationItem> operationItems = getSortedOperationItems();

        collectRequiredScopes(operationItems);

        List<ModifiableActionDefinition> actionDefinitions = new ArrayList<>();
        List<ModifiableClusterElementDefinition<?>> clusterElementDefinitions = new ArrayList<>();

        for (OperationItem operationItem : operationItems) {
            ModifiableActionDefinition actionDefinition = createActionDefinition(operationItem);

            actionDefinitions.add(actionDefinition);

            Map<String, Object> extensions = operationItem.operation()
                .getExtensions();

            if (extensions != null && Boolean.TRUE.equals(extensions.get("x-ai-agent-tool"))) {
                clusterElementDefinitions.add(tool(actionDefinition));
            }
        }

        ModifiableComponentDefinition componentDefinition = component(
            StringUtils.uncapitalize(getComponentClassName(componentName)))
                .title(title)
                .description(openAPI.getInfo() == null ? null
                    : openAPI.getInfo()
                        .getDescription())
                .version(1)
                .actions(actionDefinitions);

        ModifiableConnectionDefinition connectionDefinition = createConnectionDefinition();

        if (connectionDefinition != null) {
            componentDefinition = componentDefinition.connection(connectionDefinition);
        }

        if (!clusterElementDefinitions.isEmpty()) {
            componentDefinition = componentDefinition.clusterElements(
                clusterElementDefinitions.toArray(new ModifiableClusterElementDefinition[0]));
        }

        return componentDefinition.triggers(List.of());
    }

    //
    // Actions
    //

    private List<OperationItem> getSortedOperationItems() {
        List<OperationItem> operationItems = new ArrayList<>();

        if (openAPI.getPaths() != null) {
            openAPI.getPaths()
                .forEach((path, pathItem) -> {
                    addOperationItem(operationItems, pathItem.getDelete(), "DELETE", path);
                    addOperationItem(operationItems, pathItem.getHead(), "HEAD", path);
                    addOperationItem(operationItems, pathItem.getGet(), "GET", path);
                    addOperationItem(operationItems, pathItem.getPatch(), "PATCH", path);
                    addOperationItem(operationItems, pathItem.getPost(), "POST", path);
                    addOperationItem(operationItems, pathItem.getPut(), "PUT", path);
                });
        }

        return operationItems.stream()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getOperationId(), b.getOperationId()))
            .toList();
    }

    private static void addOperationItem(
        List<OperationItem> operationItems, Operation operation, String method, String path) {

        if (operation != null) {
            operationItems.add(new OperationItem(operation, method, path));
        }
    }

    private void collectRequiredScopes(List<OperationItem> operationItems) {
        for (OperationItem operationItem : operationItems) {
            List<SecurityRequirement> securityRequirements = operationItem.operation()
                .getSecurity();

            if (securityRequirements == null) {
                continue;
            }

            for (SecurityRequirement securityRequirement : securityRequirements) {
                if (securityRequirement.containsKey("oauth2")) {
                    requiredScopes.addAll(securityRequirement.get("oauth2"));
                }
            }
        }
    }

    private ModifiableActionDefinition createActionDefinition(OperationItem operationItem) {
        Operation operation = operationItem.operation();

        OutputEntry outputEntry = getOutputEntry(operation);
        BodyEntry bodyEntry = getBodyEntry(operation);

        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("method", operationItem.method());
        metadata.put("path", operationItem.path());

        if (bodyEntry != null && bodyEntry.bodyContentType() != null) {
            metadata.put("bodyContentType", bodyEntry.bodyContentType());
            metadata.put("mimeType", bodyEntry.mimeType());
        }

        if (outputEntry != null && outputEntry.isDynamic()) {
            metadata.put("responseType", outputEntry.dynamicResponseType());
        }

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>(getParameterProperties(operation));

        if (bodyEntry != null) {
            properties.addAll(bodyEntry.properties());
        }

        ModifiableActionDefinition actionDefinition = action(operation.getOperationId())
            .title(operation.getSummary())
            .description(operation.getDescription())
            .metadata(metadata)
            .properties(properties.toArray(new ModifiableValueProperty[0]));

        if (outputEntry != null) {
            if (outputEntry.isDynamic()) {
                actionDefinition = actionDefinition.output();
            } else if (outputEntry.outputSchema() != null) {
                if (outputEntry.sampleOutput() == null) {
                    actionDefinition = actionDefinition.output(outputSchema(outputEntry.outputSchema()));
                } else {
                    actionDefinition = actionDefinition.output(
                        outputSchema(outputEntry.outputSchema()), sampleOutput(outputEntry.sampleOutput()));
                }
            }
        }

        Map<String, Object> extensions = operation.getExtensions();

        if (extensions != null && extensions.containsKey("x-help")) {
            actionDefinition = actionDefinition.help("", (String) extensions.get("x-help"));
        }

        return actionDefinition;
    }

    private List<ModifiableValueProperty<?, ?>> getParameterProperties(Operation operation) {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Parameter parameter : resolveParameters(operation.getParameters())) {
            List<ModifiableValueProperty<?, ?>> parameterProperties = buildProperties(
                new PropertyContext(
                    parameter.getName(), parameter.getDescription(), parameter.getRequired(), parameter.getSchema(),
                    false, false, false));

            for (ModifiableValueProperty<?, ?> property : parameterProperties) {
                setMetadata(
                    property,
                    Map.of("type", PropertyType.valueOf(StringUtils.upperCase(parameter.getIn()))));

                properties.add(property);
            }
        }

        return properties;
    }

    private List<Parameter> resolveParameters(List<Parameter> parameters) {
        if (parameters == null) {
            return List.of();
        }

        List<Parameter> resolvedParameters = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter.get$ref() == null) {
                resolvedParameters.add(parameter);

                continue;
            }

            Components components = openAPI.getComponents();

            if (components == null || components.getParameters() == null) {
                throw new IllegalArgumentException(
                    "Components section of spec misses. Unable to resolve " + parameter.get$ref());
            }

            String parameterName = parameter.get$ref()
                .replace("#/components/parameters/", "");

            Parameter resolvedParameter = components.getParameters()
                .get(parameterName);

            if (resolvedParameter == null) {
                throw new IllegalArgumentException(
                    "Components section of spec misses the parameter definition for " + parameter.get$ref());
            }

            resolvedParameters.add(resolvedParameter);
        }

        return resolvedParameters;
    }

    //
    // Request body
    //

    private BodyEntry getBodyEntry(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();

        if (requestBody == null) {
            return null;
        }

        Content content = requestBody.getContent();
        Set<Map.Entry<String, MediaType>> entries = content.entrySet();

        if (entries.isEmpty()) {
            return new BodyEntry(List.of(), null, null);
        }

        String mimeType = getMimeType(entries);

        BodyContentType bodyContentType = switch (mimeType) {
            case "application/json" -> BodyContentType.JSON;
            case "application/xml" -> BodyContentType.XML;
            case "application/x-www-form-urlencoded" -> BodyContentType.FORM_URL_ENCODED;
            case "application/octet-stream" -> BodyContentType.BINARY;
            case "multipart/form-data" -> BodyContentType.FORM_DATA;
            default -> BodyContentType.RAW;
        };

        MediaType mediaType = content.get(mimeType);

        List<ModifiableValueProperty<?, ?>> properties = buildProperties(
            new PropertyContext(null, null, requestBody.getRequired(), mediaType.getSchema(), false, false, true));

        return new BodyEntry(properties, bodyContentType, mimeType);
    }

    private static String getMimeType(Set<Map.Entry<String, MediaType>> entries) {
        return entries.stream()
            .map(Map.Entry::getKey)
            .filter(curMimeType -> Objects.equals(curMimeType, "application/json"))
            .findFirst()
            .orElseGet(() -> entries.iterator()
                .next()
                .getKey());
    }

    //
    // Output
    //

    private OutputEntry getOutputEntry(Operation operation) {
        ApiResponses apiResponses = operation.getResponses();

        if (apiResponses == null) {
            return null;
        }

        ApiResponse apiResponse = null;

        for (String responseCode : List.of("200", "201", "202", "default")) {
            apiResponse = apiResponses.get(responseCode);

            if (apiResponse != null) {
                break;
            }
        }

        if (apiResponse == null) {
            return null;
        }

        if (apiResponse.getExtensions() != null) {
            Map<String, Object> extensions = apiResponse.getExtensions();

            if (Boolean.TRUE.equals(extensions.get("x-dynamic-output")) && apiResponse.getContent() != null) {
                Set<Map.Entry<String, MediaType>> entries = apiResponse.getContent()
                    .entrySet();

                if (!entries.isEmpty()) {
                    String mimeType = getMimeType(entries);

                    ResponseType responseType = switch (mimeType) {
                        case "application/json" -> ResponseType.JSON;
                        case "application/xml" -> ResponseType.XML;
                        case "application/octet-stream" -> ResponseType.BINARY;
                        default -> ResponseType.TEXT;
                    };

                    return new OutputEntry(null, null, true, responseType);
                }
            }

            return null;
        }

        if (apiResponse.getContent() == null) {
            return null;
        }

        Set<Map.Entry<String, MediaType>> entries = apiResponse.getContent()
            .entrySet();

        if (entries.isEmpty()) {
            return null;
        }

        String mimeType = getMimeType(entries);
        MediaType mediaType = apiResponse.getContent()
            .get(mimeType);

        if (mediaType.getSchema() == null) {
            return null;
        }

        return new OutputEntry(
            buildOutputSchemaProperty(mimeType, mediaType), convertSampleOutput(mediaType.getExample()), false, null);
    }

    private ModifiableValueProperty<?, ?> buildOutputSchemaProperty(String mimeType, MediaType mediaType) {
        Schema<?> schema = mediaType.getSchema();

        List<ModifiableValueProperty<?, ?>> properties = buildProperties(
            new PropertyContext(null, schema.getDescription(), null, schema, true, true, false));

        // The output-schema position holds exactly one property; the generator's code emission has the same shape.
        ModifiableValueProperty<?, ?> property = properties.getFirst();

        setMetadata(property, Map.of("responseType", getResponseType(mimeType, schema)));

        return property;
    }

    private static ResponseType getResponseType(String mimeType, Schema<?> schema) {
        if (Objects.equals(schema.getType(), "string") && Objects.equals(schema.getFormat(), "binary")) {
            return mimeType == null ? ResponseType.BINARY : ResponseType.binary(mimeType);
        }

        return switch (mimeType) {
            case "application/json" -> ResponseType.JSON;
            case "application/xml", "text/xml" -> ResponseType.XML;
            case "application/octet-stream" -> ResponseType.BINARY;
            default -> ResponseType.TEXT;
        };
    }

    //
    // Properties
    //

    /**
     * Everything {@code getSchemaCodeBlock} threads through its parameters, minus the codegen-only ones.
     *
     * @param excludePropertyNameIfEmpty mirrors the generator flag of the same name: an unnamed array/object in an
     *                                   items or output-schema position stays unnamed instead of receiving a synthetic
     *                                   name
     * @param outputSchema               suppresses editor-only attributes (labels, placeholders)
     * @param bodySchema                 marks properties that ride in the request body (BODY metadata)
     * @param sharedSchemaChildren       set when an object was resolved through a {@code $ref}: the generator renders
     *                                   such children from a shared, label-ful PROPERTIES constant regardless of the
     *                                   surrounding context, except at the body root where an inline emission bypasses
     *                                   the constant
     */
    private record PropertyContext(
        String propertyName, String propertyDescription, Boolean required, Schema<?> schema,
        boolean excludePropertyNameIfEmpty, boolean outputSchema, boolean bodySchema, boolean sharedSchemaChildren) {

        PropertyContext(
            String propertyName, String propertyDescription, Boolean required, Schema<?> schema,
            boolean excludePropertyNameIfEmpty, boolean outputSchema, boolean bodySchema) {

            this(propertyName, propertyDescription, required, schema, excludePropertyNameIfEmpty, outputSchema,
                bodySchema, false);
        }
    }

    @SuppressWarnings("rawtypes")
    private List<ModifiableValueProperty<?, ?>> buildProperties(PropertyContext context) {
        Schema<?> schema = context.schema();

        if (!StringUtils.isEmpty(schema.get$ref())) {
            return buildRefProperties(context);
        }

        String type = StringUtils.isEmpty(schema.getType()) ? "object" : schema.getType();

        ModifiableValueProperty<?, ?> property = switch (type) {
            case "array" -> buildArrayProperty(context);
            case "boolean" -> applyBodyMetadata(bool(context.propertyName()), context);
            case "integer" -> buildIntegerProperty(context);
            case "number" -> buildNumberProperty(context);
            case "object" -> buildObjectProperty(context);
            case "string" -> buildStringProperty(context);
            default -> throw new IllegalArgumentException(
                "Parameter type %s is not supported.".formatted(schema.getType()));
        };

        if (property == null) {
            // The unnamed, non-excluded object case inlines its child properties instead of wrapping them in an
            // object property, mirroring the generator's body-root emission.
            List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

            if (schema.getProperties() != null) {
                Map<String, Schema> schemaProperties = schema.getProperties();
                List<String> requiredProperties = schema.getRequired() == null ? List.of() : schema.getRequired();

                for (Map.Entry<String, Schema> entry : schemaProperties.entrySet()) {
                    Schema<?> propertySchema = entry.getValue();

                    properties.addAll(
                        buildProperties(
                            new PropertyContext(
                                entry.getKey(), propertySchema.getDescription(),
                                requiredProperties.contains(entry.getKey()), propertySchema, false,
                                context.outputSchema(), context.bodySchema())));
                }
            } else if (schema.getAllOf() != null) {
                properties.addAll(
                    buildAllOfProperties(
                        context.propertyName(), schema.getDescription(), (List) schema.getAllOf(),
                        context.outputSchema()));
            }

            return properties;
        }

        applyCommonAttributes(property, context, type);

        return List.of(property);
    }

    @SuppressWarnings("rawtypes")
    private List<ModifiableValueProperty<?, ?>> buildRefProperties(PropertyContext context) {
        Schema<?> schema = context.schema();

        Components components = openAPI.getComponents();

        if (components == null || components.getSchemas() == null) {
            throw new IllegalArgumentException("Unable to resolve schema reference " + schema.get$ref());
        }

        String schemaName = schema.get$ref()
            .replace("#/components/schemas/", "");

        Map<String, Schema> schemaMap = components.getSchemas();

        Schema<?> resolvedSchema = schemaMap.get(schemaName);

        if (resolvedSchema == null) {
            throw new IllegalArgumentException("Unable to resolve schema reference " + schema.get$ref());
        }

        boolean inlineBodyRoot = StringUtils.isEmpty(context.propertyName()) &&
            !context.excludePropertyNameIfEmpty();

        return buildProperties(
            new PropertyContext(
                context.propertyName(), resolvedSchema.getDescription(), context.required(), resolvedSchema,
                context.excludePropertyNameIfEmpty(), context.outputSchema(), context.bodySchema(),
                !inlineBodyRoot));
    }

    /**
     * Builds the property list of an object's properties map, mirroring {@code getPropertiesSchemaCodeBlock}: a child
     * whose schema declares {@code allOf} contributes its merged members inline rather than a wrapped object property,
     * and children never inherit the BODY marker.
     */
    @SuppressWarnings("rawtypes")
    private List<ModifiableValueProperty<?, ?>> buildObjectChildProperties(
        Map<String, Schema> schemaProperties, List<String> requiredProperties, boolean outputSchema) {

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Map.Entry<String, Schema> entry : schemaProperties.entrySet()) {
            Schema<?> propertySchema = entry.getValue();

            if (propertySchema.getAllOf() == null) {
                properties.addAll(
                    buildProperties(
                        new PropertyContext(
                            entry.getKey(), propertySchema.getDescription(),
                            requiredProperties.contains(entry.getKey()), propertySchema, false, outputSchema,
                            false)));
            } else {
                properties.addAll(
                    buildAllOfProperties(
                        entry.getKey(), propertySchema.getDescription(), (List) propertySchema.getAllOf(),
                        outputSchema));
            }
        }

        return properties;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private List<ModifiableValueProperty<?, ?>> buildAllOfProperties(
        String name, String description, List<Schema> allOfSchemas, boolean outputSchema) {

        Map<String, Schema> allOfProperties = getAllOfSchemaProperties(name, description, allOfSchemas);
        List<String> allOfRequired = new ArrayList<>();

        for (Schema allOfSchema : allOfProperties.values()) {
            if (allOfSchema.getRequired() != null) {
                allOfRequired.addAll(allOfSchema.getRequired());
            }
        }

        return buildObjectChildProperties(allOfProperties, allOfRequired, outputSchema);
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

    private ModifiableValueProperty<?, ?> buildArrayProperty(PropertyContext context) {
        Schema<?> schema = context.schema();

        ModifiableArrayProperty property;
        String propertyName = context.propertyName();

        if (StringUtils.isEmpty(propertyName) && context.excludePropertyNameIfEmpty()) {
            property = array();
        } else {
            propertyName = StringUtils.isEmpty(propertyName) ? "__items" : propertyName;

            property = array(propertyName);
        }

        if (schema.getItems() != null) {
            List<ModifiableValueProperty<?, ?>> itemProperties = buildProperties(
                new PropertyContext(
                    null, schema.getDescription(), null, schema.getItems(), true, context.outputSchema(),
                    context.bodySchema()));

            property.items(itemProperties.toArray(new ModifiableValueProperty[0]));
        }

        if (!context.outputSchema() && propertyName != null) {
            property.placeholder("Add to " + buildPropertyLabel(propertyName.replace("__", "")));
        }

        return applyBodyMetadata(property, context);
    }

    private ModifiableValueProperty<?, ?> buildIntegerProperty(PropertyContext context) {
        Schema<?> schema = context.schema();

        ModifiableIntegerProperty property = integer(context.propertyName());

        BigDecimal minimum = schema.getMinimum();

        if (minimum != null) {
            property.minValue(minimum.intValue());
        }

        BigDecimal maximum = schema.getMaximum();

        if (maximum != null) {
            property.maxValue(maximum.intValue());
        }

        return applyBodyMetadata(property, context);
    }

    private ModifiableValueProperty<?, ?> buildNumberProperty(PropertyContext context) {
        Schema<?> schema = context.schema();

        ModifiableNumberProperty property = number(context.propertyName());

        BigDecimal minimum = schema.getMinimum();

        if (minimum != null) {
            property.minValue(minimum.doubleValue());
        }

        BigDecimal maximum = schema.getMaximum();

        if (maximum != null) {
            property.maxValue(maximum.doubleValue());
        }

        return applyBodyMetadata(property, context);
    }

    /**
     * Returns null for the unnamed, non-excluded object case — the caller inlines the child properties, mirroring the
     * generator, which emits them as a comma-joined list rather than wrapping them in an object property.
     */
    @SuppressWarnings("rawtypes")
    private ModifiableValueProperty<?, ?> buildObjectProperty(PropertyContext context) {
        Schema<?> schema = context.schema();
        String propertyName = context.propertyName();

        if (StringUtils.isEmpty(propertyName) && !context.excludePropertyNameIfEmpty()) {
            return null;
        }

        ModifiableObjectProperty property = StringUtils.isEmpty(propertyName) ? object() : object(propertyName);

        // Children of a $ref-resolved object render label-ful regardless of the surrounding context, matching the
        // generator's shared PROPERTIES constant.
        boolean childrenOutputSchema = !context.sharedSchemaChildren() && context.outputSchema();

        if (schema.getProperties() != null || schema.getAllOf() != null) {
            List<ModifiableValueProperty<?, ?>> objectProperties = new ArrayList<>();

            if (schema.getProperties() != null) {
                objectProperties.addAll(
                    buildObjectChildProperties(
                        schema.getProperties(), schema.getRequired() == null ? List.of() : schema.getRequired(),
                        childrenOutputSchema));
            }

            if (schema.getAllOf() != null) {
                objectProperties.addAll(
                    buildAllOfProperties(
                        propertyName, schema.getDescription(), (List) schema.getAllOf(), childrenOutputSchema));
            }

            property.properties(objectProperties.toArray(new ModifiableValueProperty[0]));
        }

        if (schema.getAdditionalProperties() != null) {
            applyAdditionalProperties(property, context);
        }

        if (context.bodySchema() && !StringUtils.isEmpty(propertyName)) {
            setMetadata(property, Map.of("type", PropertyType.BODY));
        }

        return property;
    }

    private ModifiableValueProperty<?, ?> buildStringProperty(PropertyContext context) {
        Schema<?> schema = context.schema();
        String propertyName = context.propertyName();

        ModifiableValueProperty<?, ?> property;

        switch (schema.getFormat() == null ? "" : schema.getFormat()) {
            case "date" -> property = date(propertyName);
            case "date-time" -> property = dateTime(propertyName);
            case "binary" -> {
                if (context.outputSchema()) {
                    property = fileEntry();
                } else {
                    property = fileEntry(StringUtils.isEmpty(propertyName) ? "fileEntry" : propertyName);
                }
            }
            default -> {
                ModifiableStringProperty stringProperty =
                    StringUtils.isEmpty(propertyName) ? string() : string(propertyName);

                if (schema.getMinLength() != null) {
                    stringProperty.minLength(schema.getMinLength());
                }

                if (schema.getMaxLength() != null) {
                    stringProperty.maxLength(schema.getMaxLength());
                }

                property = stringProperty;
            }
        }

        return applyBodyMetadata(property, context);
    }

    private ModifiableValueProperty<?, ?> applyBodyMetadata(
        ModifiableValueProperty<?, ?> property, PropertyContext context) {

        if (context.bodySchema()) {
            setMetadata(property, Map.of("type", PropertyType.BODY));
        }

        return property;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void applyAdditionalProperties(ModifiableObjectProperty property, PropertyContext context) {
        Schema<?> schema = context.schema();

        if (schema.getAdditionalProperties() instanceof Boolean additionalProperties) {
            if (additionalProperties) {
                property.additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time());
            }
        } else {
            Schema<?> additionalPropertiesSchema = (Schema<?>) schema.getAdditionalProperties();

            if (StringUtils.isEmpty(additionalPropertiesSchema.get$ref())) {
                property.additionalProperties(buildAdditionalPropertiesItem(additionalPropertiesSchema, context));
            } else {
                String schemaName = additionalPropertiesSchema.get$ref()
                    .replace("#/components/schemas/", "");

                Components components = openAPI.getComponents();

                Schema<?> resolvedSchema = components.getSchemas()
                    .get(schemaName);

                ModifiableObjectProperty additionalProperty = object();

                List<ModifiableValueProperty<?, ?>> resolvedProperties = buildObjectChildProperties(
                    resolvedSchema.getProperties() == null ? Map.of() : resolvedSchema.getProperties(),
                    resolvedSchema.getRequired() == null ? List.of() : resolvedSchema.getRequired(), false);

                additionalProperty.properties(resolvedProperties.toArray(new ModifiableValueProperty[0]));

                property.additionalProperties(additionalProperty);
            }
        }

        if (!context.outputSchema() && context.propertyName() != null) {
            property.placeholder("Add to " + buildPropertyLabel(context.propertyName()
                .replace("__", "")));
        }
    }

    @SuppressWarnings("rawtypes")
    private ModifiableValueProperty<?, ?> buildAdditionalPropertiesItem(
        Schema<?> additionalPropertiesSchema, PropertyContext context) {

        String type = StringUtils.isEmpty(additionalPropertiesSchema.getType()) ? "object"
            : additionalPropertiesSchema.getType();

        Map<String, Schema> additionalPropertiesSchemaProperties = additionalPropertiesSchema.getProperties();

        if (Objects.equals(type, "object") && additionalPropertiesSchemaProperties != null &&
            !additionalPropertiesSchemaProperties.isEmpty()) {

            ModifiableObjectProperty objectProperty = object();

            List<ModifiableValueProperty<?, ?>> objectProperties = buildObjectChildProperties(
                additionalPropertiesSchemaProperties,
                additionalPropertiesSchema.getRequired() == null ? List.of()
                    : additionalPropertiesSchema.getRequired(),
                context.outputSchema());

            objectProperty.properties(objectProperties.toArray(new ModifiableValueProperty[0]));

            return objectProperty;
        }

        return switch (type) {
            case "array" -> array();
            case "boolean" -> bool();
            case "integer" -> integer();
            case "number" -> number();
            case "object" -> object();
            case "string" -> {
                if (StringUtils.isEmpty(additionalPropertiesSchema.getFormat())) {
                    yield string();
                } else if (Objects.equals(additionalPropertiesSchema.getFormat(), "date")) {
                    yield date();
                } else if (Objects.equals(additionalPropertiesSchema.getFormat(), "date-date")) {
                    yield dateTime();
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
    private void applyCommonAttributes(ModifiableValueProperty property, PropertyContext context, String type) {
        Schema<?> schema = context.schema();
        String propertyName = context.propertyName();

        if (!StringUtils.isEmpty(propertyName) && !context.outputSchema()) {
            property.label(
                StringUtils.isEmpty(schema.getTitle()) ? buildPropertyLabel(propertyName.replace("__", ""))
                    : schema.getTitle());
        }

        if (context.propertyDescription() != null) {
            property.description(context.propertyDescription());
        }

        if (schema.getEnum() != null && !Objects.equals(type, "boolean")) {
            applyEnumOptions(property, schema);
        }

        if (schema.getDefault() != null) {
            applyDefaultValue(property, schema, type);
        }

        if (context.required() != null && !StringUtils.isEmpty(propertyName)) {
            property.required(context.required());
        }

        if (schema.getExample() != null) {
            applyExampleValue(property, schema, type);
        }
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void applyEnumOptions(ModifiableValueProperty property, Schema<?> schema) {
        List<Option<?>> options = new ArrayList<>();

        List<?> enums = schema.getEnum()
            .stream()
            .filter(Objects::nonNull)
            .toList();

        for (Object item : enums) {
            String label = StringUtils.capitalize(item.toString());

            if (item instanceof String string) {
                options.add(option(label, string));
            } else if (item instanceof Number numberValue) {
                switch (StringUtils.defaultString(schema.getType())) {
                    case "number" -> options.add(option(label, numberValue.doubleValue()));
                    case "integer" -> {
                        if (Objects.equals(schema.getFormat(), "int64")) {
                            options.add(option(label, numberValue.longValue()));
                        } else {
                            options.add(option(label, numberValue.intValue()));
                        }
                    }
                    default -> options.add(option(label, (Object) item));
                }
            } else {
                options.add(option(label, (Object) item));
            }
        }

        if (property instanceof ModifiableStringProperty stringProperty) {
            stringProperty.options(options.toArray(new Option[0]));
        } else if (property instanceof ModifiableIntegerProperty integerProperty) {
            integerProperty.options(options.toArray(new Option[0]));
        } else if (property instanceof ModifiableNumberProperty numberProperty) {
            numberProperty.options(options.toArray(new Option[0]));
        } else if (property instanceof ModifiableArrayProperty arrayProperty) {
            arrayProperty.options(options.toArray(new Option[0]));
        }
    }

    private void applyDefaultValue(ModifiableValueProperty<?, ?> property, Schema<?> schema, String type) {
        Object defaultValue = schema.getDefault();

        if (property instanceof ModifiableStringProperty stringProperty && Objects.equals(type, "string")) {
            stringProperty.defaultValue(String.valueOf(defaultValue));
        } else if (property instanceof ModifiableIntegerProperty integerProperty &&
            defaultValue instanceof Number number) {

            integerProperty.defaultValue(number.longValue());
        } else if (property instanceof ModifiableNumberProperty numberProperty &&
            defaultValue instanceof Number number) {

            numberProperty.defaultValue(number.doubleValue());
        } else if (property instanceof ModifiableBooleanProperty booleanProperty &&
            defaultValue instanceof Boolean booleanValue) {

            booleanProperty.defaultValue(booleanValue);
        }
    }

    private void applyExampleValue(ModifiableValueProperty<?, ?> property, Schema<?> schema, String type) {
        Object example = schema.getExample();

        if (property instanceof ModifiableStringProperty stringProperty && Objects.equals(type, "string")) {
            stringProperty.exampleValue(String.valueOf(example));
        } else if (property instanceof ModifiableIntegerProperty integerProperty && example instanceof Number number) {
            integerProperty.exampleValue(number.longValue());
        } else if (property instanceof ModifiableNumberProperty numberProperty && example instanceof Number number) {
            numberProperty.exampleValue(number.doubleValue());
        }
    }

    //
    // Connection
    //

    private ModifiableConnectionDefinition createConnectionDefinition() {
        Components components = openAPI.getComponents();

        if (components == null) {
            return null;
        }

        Map<String, SecurityScheme> securitySchemeMap = components.getSecuritySchemes();
        List<Server> servers = openAPI.getServers();

        boolean securitySchemesEmpty = securitySchemeMap == null || securitySchemeMap.isEmpty();
        boolean serversEmpty = servers == null || servers.isEmpty();

        if (securitySchemesEmpty && serversEmpty) {
            return null;
        }

        ModifiableConnectionDefinition connectionDefinition = connection();

        applyBaseUri(connectionDefinition, servers);

        if (!securitySchemesEmpty) {
            List<ModifiableAuthorization> authorizations = new ArrayList<>();

            for (SecurityScheme securityScheme : securitySchemeMap.values()) {
                authorizations.addAll(createAuthorizations(securityScheme));
            }

            connectionDefinition.authorizations(authorizations.toArray(new ModifiableAuthorization[0]));
        }

        return connectionDefinition;
    }

    private void applyBaseUri(ModifiableConnectionDefinition connectionDefinition, List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return;
        }

        if (servers.size() == 1) {
            Server server = servers.getFirst();

            String url = server.getUrl();

            if (!StringUtils.isEmpty(url) && !Objects.equals(url, "/")) {
                connectionDefinition.baseUri((connectionParameters, context) -> url);
            }

            return;
        }

        List<Option<String>> options = servers.stream()
            .map(server -> option(server.getUrl(), server.getUrl()))
            .collect(Collectors.toList());

        Server firstServer = servers.getFirst();

        connectionDefinition.properties(
            string(ConnectionDefinition.BASE_URI)
                .label("Base URI")
                .options(options)
                .defaultValue(firstServer.getUrl()));
    }

    private List<ModifiableAuthorization> createAuthorizations(SecurityScheme securityScheme) {
        SecurityScheme.Type type = securityScheme.getType();

        if (type == SecurityScheme.Type.APIKEY) {
            return List.of(createApiKeyAuthorization(securityScheme));
        }

        if (type == SecurityScheme.Type.HTTP) {
            String scheme = securityScheme.getScheme();

            if (Objects.equals(scheme, "basic")) {
                return List.of(createBasicAuthorization());
            }

            if (Objects.equals(scheme, "bearer")) {
                return List.of(createBearerAuthorization(securityScheme));
            }

            throw new IllegalArgumentException("Security scheme: %s not supported".formatted(scheme));
        }

        if (type == SecurityScheme.Type.OAUTH2) {
            List<ModifiableAuthorization> authorizations = new ArrayList<>();

            OAuthFlows flows = securityScheme.getFlows();

            OAuthFlow authorizationCodeFlow = flows.getAuthorizationCode();

            if (authorizationCodeFlow != null) {
                ModifiableAuthorization authorization = createOAuth2Authorization(
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE, "OAuth2 Authorization Code", "Client Secret");

                applyAuthorizationUrl(authorization, authorizationCodeFlow);

                Map<String, Boolean> scopes = getOAuth2Scopes(authorizationCodeFlow);

                // The authorization-code emission only attaches a scopes function when the flow declares scopes; the
                // other flows always attach one.
                if (!scopes.isEmpty()) {
                    authorization.scopes((connectionParameters, context) -> scopes);
                }

                applyTokenUrl(authorization, authorizationCodeFlow);
                applyRefreshUrl(authorization, authorizationCodeFlow);

                authorizations.add(authorization);
            }

            OAuthFlow clientCredentialsFlow = flows.getClientCredentials();

            if (clientCredentialsFlow != null) {
                ModifiableAuthorization authorization = createOAuth2Authorization(
                    AuthorizationType.OAUTH2_CLIENT_CREDENTIALS, "Client Credentials", "OAuth2 Client Secret");

                Map<String, Boolean> scopes = getOAuth2Scopes(clientCredentialsFlow);

                authorization.scopes((connectionParameters, context) -> scopes);

                applyTokenUrl(authorization, clientCredentialsFlow);
                applyRefreshUrl(authorization, clientCredentialsFlow);

                authorizations.add(authorization);
            }

            OAuthFlow implicitFlow = flows.getImplicit();

            if (implicitFlow != null) {
                ModifiableAuthorization authorization = createOAuth2Authorization(
                    AuthorizationType.OAUTH2_IMPLICIT_CODE, "OAuth2 Implicit", "Client Secret");

                applyAuthorizationUrl(authorization, implicitFlow);

                Map<String, Boolean> scopes = getOAuth2Scopes(implicitFlow);

                authorization.scopes((connectionParameters, context) -> scopes);

                applyRefreshUrl(authorization, implicitFlow);

                authorizations.add(authorization);
            }

            OAuthFlow passwordFlow = flows.getPassword();

            if (passwordFlow != null) {
                ModifiableAuthorization authorization = createOAuth2Authorization(
                    AuthorizationType.OAUTH2_RESOURCE_OWNER_PASSWORD, "OAuth2 Resource Owner Password",
                    "Client Secret");

                Map<String, Boolean> scopes = getOAuth2Scopes(passwordFlow);

                authorization.scopes((connectionParameters, context) -> scopes);

                applyTokenUrl(authorization, passwordFlow);
                applyRefreshUrl(authorization, passwordFlow);

                authorizations.add(authorization);
            }

            return authorizations;
        }

        throw new IllegalArgumentException("Security scheme type=%s not supported".formatted(type));
    }

    private ModifiableAuthorization createOAuth2Authorization(
        AuthorizationType authorizationType, String title, String clientSecretLabel) {

        return authorization(authorizationType)
            .title(title)
            .properties(
                string(Authorization.CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(Authorization.CLIENT_SECRET)
                    .label(clientSecretLabel)
                    .required(true));
    }

    private static void applyAuthorizationUrl(ModifiableAuthorization authorization, OAuthFlow oAuthFlow) {
        String authorizationUrl = oAuthFlow.getAuthorizationUrl();

        authorization.authorizationUrl((connectionParameters, context) -> authorizationUrl);
    }

    private static void applyTokenUrl(ModifiableAuthorization authorization, OAuthFlow oAuthFlow) {
        String tokenUrl = oAuthFlow.getTokenUrl();

        authorization.tokenUrl((connectionParameters, context) -> tokenUrl);
    }

    private static void applyRefreshUrl(ModifiableAuthorization authorization, OAuthFlow oAuthFlow) {
        String refreshUrl = oAuthFlow.getRefreshUrl();

        if (refreshUrl != null) {
            authorization.refreshUrl((connectionParameters, context) -> refreshUrl);
        }
    }

    private ModifiableAuthorization createApiKeyAuthorization(SecurityScheme securityScheme) {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        String securitySchemeName = securityScheme.getName();

        if (!StringUtils.isEmpty(securitySchemeName) && !Objects.equals(securitySchemeName, "api_token")) {
            properties.add(
                string(Authorization.KEY)
                    .label("Key")
                    .required(true)
                    .defaultValue(securitySchemeName)
                    .hidden(true));
        }

        String label = "Value";

        Map<String, Object> extensions = securityScheme.getExtensions();

        if (extensions != null && extensions.get("x-title") != null) {
            label = (String) extensions.get("x-title");
        }

        properties.add(
            string(Authorization.VALUE)
                .label(label)
                .required(true));

        if (securityScheme.getIn() != SecurityScheme.In.HEADER) {
            properties.add(
                string(Authorization.ADD_TO)
                    .label("Add to")
                    .required(true)
                    .defaultValue(ApiTokenLocation.QUERY_PARAMETERS.name())
                    .hidden(true));
        }

        return authorization(AuthorizationType.API_KEY)
            .title("API Key")
            .properties(properties.toArray(new ModifiableValueProperty[0]));
    }

    private ModifiableAuthorization createBasicAuthorization() {
        return authorization(AuthorizationType.BASIC_AUTH)
            .title("Basic Auth")
            .properties(
                string(Authorization.USERNAME)
                    .label("Username")
                    .required(true),
                string(Authorization.PASSWORD)
                    .label("Password")
                    .controlType(ControlType.PASSWORD)
                    .required(true));
    }

    private ModifiableAuthorization createBearerAuthorization(SecurityScheme securityScheme) {
        String label = "Token";

        Map<String, Object> extensions = securityScheme.getExtensions();

        if (extensions != null && extensions.get("x-title") != null) {
            label = (String) extensions.get("x-title");
        }

        return authorization(AuthorizationType.BEARER_TOKEN)
            .title("Bearer Token")
            .properties(
                string(Authorization.TOKEN)
                    .label(label)
                    .required(true));
    }

    private Map<String, Boolean> getOAuth2Scopes(OAuthFlow oAuthFlow) {
        if (oAuthFlow.getScopes() == null) {
            return Map.of();
        }

        Map<String, Boolean> scopes = new LinkedHashMap<>();

        for (String scopeName : oAuthFlow.getScopes()
            .keySet()) {

            scopes.put(scopeName, requiredScopes.contains(scopeName));
        }

        return scopes;
    }

    //
    // Sample-output conversion — the value-conversion half of the generator's getSampleOutputCodeBlock
    //

    private static Object convertSampleOutput(Object sampleOutput) {
        switch (sampleOutput) {
            case null -> {
                return null;
            }
            case String string -> {
                Object convertedSampleOutput = null;
                JsonNode jsonNode = null;

                try {
                    jsonNode = OBJECT_MAPPER.readTree(string);
                } catch (JsonProcessingException exception) {
                    // Not JSON; fall through to the scalar-conversion chain.
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
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<LocalDateTime>() {});
                }

                if (convertedSampleOutput == null) {
                    convertedSampleOutput = convert(sampleOutput, new TypeReference<LocalDate>() {});
                }

                return convertedSampleOutput == null ? sampleOutput : convertedSampleOutput;
            }
            case JsonNode jsonNode -> {
                Object converted = convert(jsonNode, new TypeReference<Map<String, Object>>() {});

                if (converted == null) {
                    converted = convert(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
                }

                return converted == null ? sampleOutput : converted;
            }
            default -> {
                return sampleOutput;
            }
        }
    }

    private static <T> Object convert(Object fromValue, TypeReference<T> toValueType) {
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    //
    // Naming helpers, ported verbatim
    //

    private static String getComponentClassName(String componentName) {
        return Arrays.stream(componentName.split("-"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining());
    }

    private static String buildPropertyLabel(String propertyName) {
        return Arrays.stream(StringUtils.split(propertyName, '_'))
            .flatMap(item -> Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(item)))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    @SuppressWarnings("rawtypes")
    private static void setMetadata(ModifiableValueProperty property, Map<String, Object> metadata) {
        property.metadata(metadata);
    }

    private record OperationItem(Operation operation, String method, String path) {
        String getOperationId() {
            return operation.getOperationId();
        }
    }

    private record BodyEntry(
        List<ModifiableValueProperty<?, ?>> properties, BodyContentType bodyContentType, String mimeType) {
    }

    private record OutputEntry(
        ModifiableValueProperty<?, ?> outputSchema, Object sampleOutput, boolean isDynamic,
        ResponseType dynamicResponseType) {
    }
}

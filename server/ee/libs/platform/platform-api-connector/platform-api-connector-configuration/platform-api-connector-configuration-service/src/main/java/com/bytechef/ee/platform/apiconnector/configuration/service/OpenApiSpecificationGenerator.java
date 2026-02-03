/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.dto.EndpointDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.GenerateSpecificationRequestDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ParameterDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.RequestBodyDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ResponseDefinitionDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Service for generating OpenAPI specifications from endpoint definitions.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class OpenApiSpecificationGenerator {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public OpenApiSpecificationGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(GenerateSpecificationRequestDTO request) {
        Map<String, Object> spec = new LinkedHashMap<>();

        spec.put("openapi", "3.0.0");

        Map<String, Object> info = new LinkedHashMap<>();

        info.put("title", request.name());
        info.put("version", "1.0.0");

        spec.put("info", info);

        if (request.baseUrl() != null && !request.baseUrl()
            .isEmpty()) {
            Map<String, Object> server = new LinkedHashMap<>();

            server.put("url", request.baseUrl());

            spec.put("servers", List.of(server));
        }

        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();

        if (request.endpoints() != null) {
            for (EndpointDefinitionDTO endpoint : request.endpoints()) {
                String path = endpoint.path();
                String method = endpoint.httpMethod()
                    .toLowerCase();

                Map<String, Object> pathItem = paths.computeIfAbsent(path, pathKey -> new LinkedHashMap<>());

                Map<String, Object> operation = buildOperation(endpoint);

                pathItem.put(method, operation);
            }
        }

        spec.put("paths", paths);

        return toYaml(spec);
    }

    private Map<String, Object> buildOperation(EndpointDefinitionDTO endpoint) {
        Map<String, Object> operation = new LinkedHashMap<>();

        operation.put("operationId", endpoint.operationId());

        if (endpoint.summary() != null && !endpoint.summary()
            .isEmpty()) {
            operation.put("summary", endpoint.summary());
        }

        if (endpoint.description() != null && !endpoint.description()
            .isEmpty()) {
            operation.put("description", endpoint.description());
        }

        List<ParameterDefinitionDTO> parameters = endpoint.parameters();

        if (parameters != null && !parameters.isEmpty()) {
            operation.put("parameters", buildParameters(parameters));
        }

        RequestBodyDefinitionDTO requestBody = endpoint.requestBody();

        if (requestBody != null) {
            operation.put("requestBody", buildRequestBody(requestBody));
        }

        Map<String, Object> responses = buildResponses(endpoint.responses());

        operation.put("responses", responses);

        return operation;
    }

    private List<Map<String, Object>> buildParameters(List<ParameterDefinitionDTO> parameters) {
        return parameters.stream()
            .map(param -> {
                Map<String, Object> paramMap = new LinkedHashMap<>();

                paramMap.put("name", param.name());
                paramMap.put("in", param.in());

                if (param.description() != null && !param.description()
                    .isEmpty()) {
                    paramMap.put("description", param.description());
                }

                if (param.required() != null && param.required()) {
                    paramMap.put("required", true);
                }

                Map<String, Object> schema = new LinkedHashMap<>();

                schema.put("type", param.type());

                paramMap.put("schema", schema);

                if (param.example() != null && !param.example()
                    .isEmpty()) {
                    paramMap.put("example", param.example());
                }

                return paramMap;
            })
            .toList();
    }

    private Map<String, Object> buildRequestBody(RequestBodyDefinitionDTO requestBody) {
        Map<String, Object> requestBodyMap = new LinkedHashMap<>();

        if (requestBody.description() != null && !requestBody.description()
            .isEmpty()) {
            requestBodyMap.put("description", requestBody.description());
        }

        if (requestBody.required() != null && requestBody.required()) {
            requestBodyMap.put("required", true);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        Map<String, Object> mediaType = new LinkedHashMap<>();

        String schemaString = requestBody.schema();

        if (schemaString != null && !schemaString.isEmpty()) {
            try {
                JsonNode schemaNode = objectMapper.readTree(schemaString);

                mediaType.put("schema", objectMapper.convertValue(schemaNode, Map.class));
            } catch (JsonProcessingException e) {
                Map<String, Object> fallbackSchema = new LinkedHashMap<>();

                fallbackSchema.put("type", "object");

                mediaType.put("schema", fallbackSchema);
            }
        } else {
            Map<String, Object> emptySchema = new LinkedHashMap<>();

            emptySchema.put("type", "object");

            mediaType.put("schema", emptySchema);
        }

        content.put(requestBody.contentType(), mediaType);

        requestBodyMap.put("content", content);

        return requestBodyMap;
    }

    private Map<String, Object> buildResponses(List<ResponseDefinitionDTO> responses) {
        Map<String, Object> responsesMap = new LinkedHashMap<>();

        if (responses == null || responses.isEmpty()) {
            Map<String, Object> defaultResponse = new LinkedHashMap<>();

            defaultResponse.put("description", "Successful response");

            responsesMap.put("200", defaultResponse);

            return responsesMap;
        }

        for (ResponseDefinitionDTO response : responses) {
            Map<String, Object> responseMap = new LinkedHashMap<>();

            responseMap.put("description", response.description());

            if (response.contentType() != null && !response.contentType()
                .isEmpty()) {
                Map<String, Object> content = new LinkedHashMap<>();
                Map<String, Object> mediaType = new LinkedHashMap<>();

                String schemaString = response.schema();

                if (schemaString != null && !schemaString.isEmpty()) {
                    try {
                        JsonNode schemaNode = objectMapper.readTree(schemaString);

                        mediaType.put("schema", objectMapper.convertValue(schemaNode, Map.class));
                    } catch (JsonProcessingException e) {
                        Map<String, Object> fallbackSchema = new LinkedHashMap<>();

                        fallbackSchema.put("type", "object");

                        mediaType.put("schema", fallbackSchema);
                    }
                }

                content.put(response.contentType(), mediaType);

                responseMap.put("content", content);
            }

            responsesMap.put(response.statusCode(), responseMap);
        }

        return responsesMap;
    }

    private String toYaml(Map<String, Object> spec) {
        DumperOptions options = new DumperOptions();

        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);

        Yaml yaml = new Yaml(options);

        return yaml.dump(spec);
    }
}

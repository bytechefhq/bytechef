/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint.HttpMethod;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import com.bytechef.ee.platform.apiconnector.configuration.exception.ApiConnectorErrorType;
import com.bytechef.ee.platform.apiconnector.configuration.generator.OpenApiGenerator;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ApiConnectorFacadeImpl implements ApiConnectorFacade {

    private final ApiConnectorFileStorage apiConnectorFileStorage;
    private final ApiConnectorService apiConnectorService;

    @SuppressFBWarnings("EI")
    public ApiConnectorFacadeImpl(
        ApiConnectorFileStorage apiConnectorFileStorage, ApiConnectorService apiConnectorService) {

        this.apiConnectorFileStorage = apiConnectorFileStorage;
        this.apiConnectorService = apiConnectorService;
    }

    @Override
    public ApiConnector importOpenApiSpecification(String name, String specification) {
        name = convertComponentName(name);

        Path openApiSpecificationPath;

        try {
            openApiSpecificationPath = Files.createTempFile("open_api_specification", ".yaml");
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        try {
            Files.writeString(openApiSpecificationPath, specification);
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        OpenAPI openAPI = parseOpenAPIFile(openApiSpecificationPath.toString());

        Path definitionFilePath;

        try {
            definitionFilePath = OpenApiGenerator.generate(name, openApiSpecificationPath);
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        ApiConnector apiConnector = apiConnectorService.fetchApiConnector(name, 1)
            .orElse(null);
        boolean isNew = false;

        if (apiConnector == null) {
            apiConnector = new ApiConnector();
            isNew = true;
        }

        apiConnector.setName(name);
        apiConnector.setConnectorVersion(1);

        try {
            apiConnector.setDefinition(
                apiConnectorFileStorage.storeApiConnectorDefinition(
                    "definition.json", Files.readString(definitionFilePath)));
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        apiConnector.setDescription(getDescription(openAPI.getInfo()));
        apiConnector.setSpecification(
            apiConnectorFileStorage.storeApiConnectorSpecification("specification.yaml", specification));
        apiConnector.setTitle(getTitle(name, openAPI.getInfo()));

        if (isNew) {
            return apiConnectorService.create(apiConnector);
        } else {
            return apiConnectorService.update(apiConnector);
        }
    }

    @Override
    public List<ApiConnectorDTO> getApiConnectors() {
        return apiConnectorService.getApiConnectors()
            .stream()
            .map(this::toApiConnectorDTO)
            .toList();
    }

    private ApiConnectorDTO toApiConnectorDTO(ApiConnector apiConnector) {
        String specification = apiConnectorFileStorage.readApiConnectorSpecification(apiConnector.getSpecification());

        OpenAPI openAPI = parseOpenAPIContent(specification);

        List<ApiConnectorEndpoint> endpoints = openAPI.getPaths()
            .entrySet()
            .stream()
            .flatMap(entry -> {
                List<ApiConnectorEndpoint> curEndpoints = new ArrayList<>();

                String path = entry.getKey();
                PathItem pathItem = entry.getValue();

                if (pathItem.getDelete() != null) {
                    Operation operation = pathItem.getDelete();

                    curEndpoints.add(
                        new ApiConnectorEndpoint(
                            path, operation.getOperationId(), operation.getDescription(), HttpMethod.DELETE));
                }

                if (pathItem.getGet() != null) {
                    Operation operation = pathItem.getGet();

                    curEndpoints.add(
                        new ApiConnectorEndpoint(
                            path, operation.getOperationId(), operation.getDescription(), HttpMethod.GET));
                }

                if (pathItem.getPatch() != null) {
                    Operation operation = pathItem.getPatch();

                    curEndpoints.add(
                        new ApiConnectorEndpoint(
                            path, operation.getOperationId(), operation.getDescription(), HttpMethod.PATCH));
                }

                if (pathItem.getPost() != null) {
                    Operation operation = pathItem.getPost();

                    curEndpoints.add(
                        new ApiConnectorEndpoint(
                            path, operation.getOperationId(), operation.getDescription(), HttpMethod.POST));
                }

                if (pathItem.getPut() != null) {
                    Operation operation = pathItem.getPut();

                    curEndpoints.add(
                        new ApiConnectorEndpoint(
                            path, operation.getOperationId(), operation.getDescription(), HttpMethod.PUT));
                }

                return CollectionUtils.stream(curEndpoints);
            })
            .toList();

        return new ApiConnectorDTO(
            apiConnector, apiConnectorFileStorage.readApiConnectorDefinition(apiConnector.getDefinition()),
            specification, endpoints);
    }

    private static String convertComponentName(String componentName) {
        componentName = StringUtils.trim(componentName);

        componentName = componentName.toLowerCase();

        componentName = StringUtils.replaceChars(componentName, "-_", ".");

        componentName = StringUtils.replaceChars(componentName, " ", "");

        return componentName;
    }

    private String getDescription(Info info) {
        return info.getDescription();
    }

    private String getTitle(String componentName, Info info) {
        String title = info.getTitle();

        if (StringUtils.isEmpty(title)) {
            String[] items = componentName.split("-");

            title = Arrays.stream(items)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(""));
        }

        return title;
    }

    private OpenAPI parseOpenAPIFile(String openApiPath) {
        SwaggerParseResult result = new OpenAPIParser().readLocation(openApiPath, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        List<String> messages = result.getMessages();

        if (messages != null && !messages.isEmpty()) {
            throw new ConfigurationException(
                String.join("\n", messages), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return openAPI;
    }

    private OpenAPI parseOpenAPIContent(String specification) {
        SwaggerParseResult result = new OpenAPIParser().readContents(specification, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        List<String> messages = result.getMessages();

        if (messages != null && !messages.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", messages));
        }

        return openAPI;
    }
}

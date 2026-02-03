/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.EndpointDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.GenerateSpecificationRequestDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ParameterDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.RequestBodyDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ResponseDefinitionDTO;
import com.bytechef.ee.platform.apiconnector.configuration.exception.ApiConnectorErrorType;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.configuration.service.OpenApiSpecificationGenerator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing API Connectors.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ApiConnectorGraphQlController {

    private final ApiConnectorAiService apiConnectorAiService;
    private final ApiConnectorFacade apiConnectorFacade;
    private final ApiConnectorGenerationJobService apiConnectorGenerationJobService;
    private final ApiConnectorService apiConnectorService;
    private final OpenApiSpecificationGenerator openApiSpecificationGenerator;

    @SuppressFBWarnings("EI")
    public ApiConnectorGraphQlController(
        Optional<ApiConnectorAiService> apiConnectorAiService, ApiConnectorFacade apiConnectorFacade,
        ApiConnectorGenerationJobService apiConnectorGenerationJobService, ApiConnectorService apiConnectorService,
        OpenApiSpecificationGenerator openApiSpecificationGenerator) {

        this.apiConnectorAiService = apiConnectorAiService.orElse(null);
        this.apiConnectorFacade = apiConnectorFacade;
        this.apiConnectorGenerationJobService = apiConnectorGenerationJobService;
        this.apiConnectorService = apiConnectorService;
        this.openApiSpecificationGenerator = openApiSpecificationGenerator;
    }

    @QueryMapping
    public ApiConnector apiConnector(@Argument long id) {
        return apiConnectorService.getApiConnector(id);
    }

    @QueryMapping
    public List<ApiConnectorDTO> apiConnectors() {
        return apiConnectorFacade.getApiConnectors();
    }

    @QueryMapping
    public GenerationJobStatusRecord generationJobStatus(@Argument String jobId) {
        return apiConnectorGenerationJobService.get(jobId)
            .map(this::toGenerationJobStatusRecord)
            .orElse(null);
    }

    @MutationMapping
    public ApiConnector createApiConnector(@Argument CreateApiConnectorInput input) {
        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setName(input.name());
        apiConnector.setTitle(input.title());
        apiConnector.setDescription(input.description());
        apiConnector.setIcon(input.icon());
        apiConnector.setConnectorVersion(input.connectorVersion());

        return apiConnectorService.create(apiConnector);
    }

    @MutationMapping
    public boolean deleteApiConnector(@Argument long id) {
        apiConnectorService.delete(id);

        return true;
    }

    @MutationMapping
    public boolean enableApiConnector(@Argument long id, @Argument boolean enable) {
        apiConnectorService.enableApiConnector(id, enable);

        return true;
    }

    @MutationMapping
    public ApiConnector updateApiConnector(@Argument long id, @Argument UpdateApiConnectorInput input) {
        ApiConnector apiConnector = apiConnectorService.getApiConnector(id);

        if (input.name() != null) {
            apiConnector.setName(input.name());
        }

        if (input.title() != null) {
            apiConnector.setTitle(input.title());
        }

        if (input.description() != null) {
            apiConnector.setDescription(input.description());
        }

        if (input.icon() != null) {
            apiConnector.setIcon(input.icon());
        }

        if (input.connectorVersion() != null) {
            apiConnector.setConnectorVersion(input.connectorVersion()
                .intValue());
        }

        return apiConnectorService.update(apiConnector);
    }

    @MutationMapping
    public ApiConnector importOpenApiSpecification(@Argument ImportOpenApiSpecificationInput input) {
        return apiConnectorFacade.importOpenApiSpecification(input.name(), input.specification());
    }

    @MutationMapping
    public GenerateSpecificationResponseRecord generateSpecification(@Argument GenerateSpecificationInput input) {
        GenerateSpecificationRequestDTO requestDTO = toDTO(input);

        String specification = openApiSpecificationGenerator.generate(requestDTO);

        return new GenerateSpecificationResponseRecord(specification);
    }

    @MutationMapping
    public ApiConnector generateFromDocumentation(@Argument GenerateFromDocumentationInput input) {
        return apiConnectorFacade.generateFromDocumentation(input.name(), input.documentationUrl());
    }

    @MutationMapping
    public GenerationJobStatusRecord startGenerateFromDocumentationPreview(
        @Argument GenerateFromDocumentationInput input) {

        if (apiConnectorAiService == null) {
            throw new ConfigurationException(
                "AI service is not configured. Please configure bytechef.ai.copilot settings.",
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        ApiConnectorGenerationJob job = apiConnectorGenerationJobService.create(input.documentationUrl());

        apiConnectorAiService.generateOpenApiSpecificationAsync(job.getId(), input.documentationUrl(),
            input.userPrompt());

        return toGenerationJobStatusRecord(job);
    }

    @MutationMapping
    public boolean cancelGenerationJob(@Argument String jobId) {
        return apiConnectorGenerationJobService.requestCancellation(jobId);
    }

    private GenerationJobStatusRecord toGenerationJobStatusRecord(ApiConnectorGenerationJob job) {
        return new GenerationJobStatusRecord(
            job.getId(),
            job.getStatus()
                .name(),
            job.getSpecification(),
            job.getErrorMessage());
    }

    private GenerateSpecificationRequestDTO toDTO(GenerateSpecificationInput input) {
        List<EndpointDefinitionDTO> endpoints = CollectionUtils.map(input.endpoints(), this::toDTO);

        return new GenerateSpecificationRequestDTO(input.name(), input.baseUrl(), endpoints);
    }

    private EndpointDefinitionDTO toDTO(EndpointDefinitionInput input) {
        List<ParameterDefinitionDTO> parameters = CollectionUtils.map(input.parameters(), this::toDTO);
        RequestBodyDefinitionDTO requestBody = input.requestBody() != null ? toDTO(input.requestBody()) : null;
        List<ResponseDefinitionDTO> responses = CollectionUtils.map(input.responses(), this::toDTO);

        return new EndpointDefinitionDTO(
            input.path(),
            input.httpMethod(),
            input.operationId(),
            input.summary(),
            input.description(),
            parameters,
            requestBody,
            responses);
    }

    private ParameterDefinitionDTO toDTO(ParameterDefinitionInput input) {
        return new ParameterDefinitionDTO(
            input.name(),
            input.location(),
            input.type(),
            input.description(),
            input.required(),
            input.example());
    }

    private RequestBodyDefinitionDTO toDTO(RequestBodyDefinitionInput input) {
        return new RequestBodyDefinitionDTO(
            input.contentType(),
            input.description(),
            input.required(),
            input.schema());
    }

    private ResponseDefinitionDTO toDTO(ResponseDefinitionInput input) {
        return new ResponseDefinitionDTO(
            input.statusCode(),
            input.description(),
            input.contentType(),
            input.schema());
    }

    @SuppressFBWarnings("EI")
    public record CreateApiConnectorInput(
        String name, String title, String description, String icon, int connectorVersion) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateApiConnectorInput(
        String name, String title, String description, String icon, Integer connectorVersion) {
    }

    @SuppressFBWarnings("EI")
    public record ImportOpenApiSpecificationInput(String name, String icon, String specification) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateFromDocumentationInput(String name, String documentationUrl, String icon, String userPrompt) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateSpecificationInput(String name, String baseUrl, List<EndpointDefinitionInput> endpoints) {
    }

    @SuppressFBWarnings("EI")
    public record EndpointDefinitionInput(
        String path, String httpMethod, String operationId, String summary, String description,
        List<ParameterDefinitionInput> parameters, RequestBodyDefinitionInput requestBody,
        List<ResponseDefinitionInput> responses) {
    }

    @SuppressFBWarnings("EI")
    public record ParameterDefinitionInput(
        String name, String location, String type, String description, Boolean required, String example) {
    }

    @SuppressFBWarnings("EI")
    public record RequestBodyDefinitionInput(
        String contentType, String description, Boolean required, String schema) {
    }

    @SuppressFBWarnings("EI")
    public record ResponseDefinitionInput(String statusCode, String description, String contentType, String schema) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateSpecificationResponseRecord(String specification) {
    }

    @SuppressFBWarnings("EI")
    public record GenerationJobStatusRecord(
        String jobId, String status, String specification, String errorMessage) {
    }
}

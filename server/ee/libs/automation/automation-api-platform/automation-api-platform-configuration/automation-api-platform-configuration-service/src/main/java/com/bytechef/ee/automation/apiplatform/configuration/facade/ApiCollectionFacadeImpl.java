/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiCollectionFacadeImpl implements ApiCollectionFacade {

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final EnvironmentService environmentService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApiCollectionFacadeImpl(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        EnvironmentService environmentService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService, WorkflowService workflowService) {

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.environmentService = environmentService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public ApiCollectionDTO createApiCollection(ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setDescription(apiCollection.getDescription());
        projectDeployment.setEnvironment(apiCollectionDTO.environment());
        projectDeployment.setName("__API_COLLECTION__" + apiCollectionDTO.projectId());
        projectDeployment.setProjectId(apiCollectionDTO.projectId());
        projectDeployment.setProjectVersion(apiCollectionDTO.projectVersion());

        List<Tag> tags = checkTags(apiCollectionDTO.tags());

        if (!tags.isEmpty()) {
            apiCollection.setTags(tags);
        }

        projectDeployment = projectDeploymentService.create(projectDeployment);

        apiCollection.setProjectDeploymentId(projectDeployment.getId());

        return toApiCollectionDTO(apiCollectionService.create(apiCollection));
    }

    @Override
    public ApiCollectionEndpointDTO createApiCollectionEndpoint(
        ApiCollectionEndpointDTO apiCollectionEndpointDTO) {

        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        ApiCollection apiCollection = apiCollectionService.getApiCollection(
            apiCollectionEndpoint.getApiCollectionId());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .fetchProjectDeploymentWorkflow(
                apiCollection.getProjectDeploymentId(),
                projectWorkflowService.getProjectWorkflowWorkflowId(
                    apiCollection.getProjectDeploymentId(), apiCollectionEndpointDTO.workflowUuid()))
            .orElseGet(() -> {
                ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                newProjectDeploymentWorkflow.setProjectDeploymentId(apiCollection.getProjectDeploymentId());
                newProjectDeploymentWorkflow.setWorkflowId(
                    projectWorkflowService.getProjectWorkflowWorkflowId(
                        apiCollection.getProjectDeploymentId(), apiCollectionEndpointDTO.workflowUuid()));

                return projectDeploymentWorkflowService.create(newProjectDeploymentWorkflow);
            });

        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflow.getId());

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpointService.create(apiCollectionEndpoint), projectDeploymentWorkflow.isEnabled(),
            apiCollectionEndpointDTO.workflowUuid());
    }

    @Override
    public void deleteApiCollection(long id) {
        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(id)) {
            apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
        }

        apiCollectionService.delete(id);
        projectDeploymentFacade.deleteProjectDeployment(apiCollection.getProjectDeploymentId());
    }

    @Override
    public ApiCollectionDTO getApiCollection(long id) {
        return toApiCollectionDTO(apiCollectionService.getApiCollection(id));
    }

    @Override
    public List<ApiCollectionDTO> getApiCollections(
        long workspaceId, Long environmentId, Long projectId, Long tagId) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        return apiCollectionService.getApiCollections(workspaceId, environment, projectId, tagId)
            .stream()
            .map(this::toApiCollectionDTO)
            .toList();
    }

    @Override
    public List<Tag> getApiCollectionTags() {
        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(null, null, null, null);

        return tagService.getTags(
            apiCollections.stream()
                .map(ApiCollection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public String getOpenApiSpecification(long id) {
        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        OpenAPI openAPI = new OpenAPI();

        openAPI.info(
            new Info()
                .title(apiCollection.getName())
                .version(String.valueOf(apiCollection.getCollectionVersion())));

        List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(id);

        Map<String, PathItem> pathItemMap = new HashMap<>();

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflow(apiCollectionEndpoint.getProjectDeploymentWorkflowId());

            Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

            WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflow)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow trigger not found"));

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (!Objects.equals(workflowNodeType.operation(), "newApiRequest")) {
                throw new ConfigurationException(
                    "Invalid workflow trigger type", ApiCollectionErrorType.INVALID_WORKFLOW_TRIGGER_TYPE);
            }

            String path = createPath(apiCollectionEndpoint, apiCollection);
            Map<String, ?> parameters = workflowTrigger.getParameters();

            PathItem pathItem = pathItemMap.computeIfAbsent(path, k -> new PathItem());

            HttpMethod httpMethod = switch (apiCollectionEndpoint.getHttpMethod()) {
                case GET -> HttpMethod.GET;
                case POST -> HttpMethod.POST;
                case PUT -> HttpMethod.PUT;
                case DELETE -> HttpMethod.DELETE;
                case PATCH -> HttpMethod.PATCH;
            };

            buildPathItem(pathItem, httpMethod, path, workflow.getDescription(), apiCollectionEndpoint, parameters);
        }

        pathItemMap.forEach(openAPI::path);

        return Json.pretty(openAPI);
    }

    @Override
    public List<Project> getWorkspaceProjects(long workspaceId) {
        return projectService.getProjects(apiCollectionService.getApiCollectionProjectIds(workspaceId));
    }

    @Override
    public ApiCollectionDTO updateApiCollection(ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        List<Tag> tags = checkTags(apiCollectionDTO.tags());

        if (!tags.isEmpty()) {
            apiCollection.setTags(tags);
        }

        return toApiCollectionDTO(apiCollectionService.update(apiCollection));
    }

    @Override
    public ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO) {
        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        apiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        return toApiCollectionEndpointDTO(apiCollectionEndpoint);
    }

    @Override
    public void updateApiCollectionTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        apiCollectionService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    private void buildPathItem(
        PathItem pathItem, HttpMethod httpMethod, String path, String description,
        ApiCollectionEndpoint apiCollectionEndpoint, Map<String, ?> parameters) {

        Operation operation = new Operation();

        operation.description(description);

        String name = httpMethod.name();

        operation.operationId(name.toLowerCase() + StringUtils.capitalize(apiCollectionEndpoint.getName()));

        operation.parameters(createParameters(path, parameters));
        operation.requestBody(createRequestBody(parameters));
        operation.responses(createApiResponses(parameters));

        pathItem.operation(httpMethod, operation);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    @SuppressWarnings("unchecked")
    private static ApiResponse createApiResponse(String jsonSchema, String description) {
        Map<String, ?> jsonSchemaMap = JsonUtils.readMap(jsonSchema);

        return new ApiResponse()
            .description(description)
            .content(
                new Content().addMediaType(
                    "application/json",
                    new MediaType().schema(
                        new Schema<>()
                            .type(MapUtils.getFromPath(jsonSchemaMap, "type", String.class))
                            .properties(MapUtils.getFromPath(jsonSchemaMap, "properties", new TypeReference<>() {})))));
    }

    @SuppressWarnings("unchecked")
    private static ApiResponse createDefaultInternalErrrorApiResponse() {
        return new ApiResponse()
            .description("Internal error")
            .content(
                new Content().addMediaType(
                    "application/json",
                    new MediaType().schema(
                        new Schema<>()
                            .type("object")
                            .properties(
                                Map.of(
                                    "message",
                                    new Schema<>()
                                        .type("string")
                                        .description("Error message"))))));
    }

    private static String createPath(ApiCollectionEndpoint apiCollectionEndpoint, ApiCollection apiCollection) {
        return "/v" + apiCollection.getCollectionVersion() + "/" + apiCollection.getContextPath() +
            (StringUtils.isEmpty(apiCollectionEndpoint.getPath()) ? "" : apiCollectionEndpoint.getPath());
    }

    private static List<Parameter> createParameters(String path, Map<String, ?> parameterMap) {
        List<Parameter> parameters = new ArrayList<>();

        String requestHeadersJsonSchema = MapUtils.getFromPath(parameterMap, "request.headers", String.class);

        if (requestHeadersJsonSchema != null) {
            parameters.addAll(createParameters(null, "header", requestHeadersJsonSchema));
        }

        String requestParametersJsonSchema = MapUtils.getFromPath(parameterMap, "request.parameters", String.class);

        if (requestParametersJsonSchema != null) {
            parameters.addAll(createParameters(path, null, requestParametersJsonSchema));
        }

        return parameters;
    }

    private static List<Parameter> createParameters(String path, String in, String jsonSchema) {
        List<Parameter> parameters = new ArrayList<>();

        Map<String, ?> jsonSchemaMap = JsonUtils.readMap(jsonSchema);

        Map<String, Map<String, ?>> properties = MapUtils.getMap(
            jsonSchemaMap, "properties", new TypeReference<>() {}, Map.of());
        List<String> required = MapUtils.getList(jsonSchemaMap, "required", String.class, List.of());

        for (Map.Entry<String, Map<String, ?>> entry : properties.entrySet()) {
            Parameter parameter = new Parameter();

            parameter.name(entry.getKey());
            parameter.required(required.contains(entry.getKey()));
            parameter.in(in == null ? path.contains("{" + entry.getKey() + "}") ? "path" : "query" : in);
            parameter.description(MapUtils.getFromPath(entry.getValue(), "description", String.class));
            parameter.schema(
                new Schema<>()
                    .type(MapUtils.getFromPath(entry.getValue(), "type", String.class))
                    .format(MapUtils.getFromPath(entry.getValue(), "format", String.class)));

            parameters.add(parameter);
        }

        return parameters;
    }

    @SuppressWarnings("unchecked")
    private static RequestBody createRequestBody(Map<String, ?> parameterMap) {
        RequestBody requestBody = null;
        String requestBodyJsonSchema = MapUtils.getFromPath(parameterMap, "request.body", new TypeReference<>() {});

        if (requestBodyJsonSchema != null) {
            requestBody = new RequestBody();

            Map<String, ?> jsonSchemaMap = JsonUtils.readMap(requestBodyJsonSchema);

            requestBody.content(
                new Content().addMediaType(
                    "application/json",
                    new MediaType().schema(
                        new Schema<>()
                            .type(MapUtils.getFromPath(jsonSchemaMap, "type", String.class))
                            .properties(MapUtils.getFromPath(jsonSchemaMap, "properties", new TypeReference<>() {})))));
        }

        return requestBody;
    }

    private static ApiResponses createApiResponses(Map<String, ?> parameterMap) {
        ApiResponses apiResponses = new ApiResponses();

        String responseSuccessJsonSchema = MapUtils.getFromPath(parameterMap, "response.success", String.class);

        if (responseSuccessJsonSchema != null) {
            apiResponses.put("200", createApiResponse(responseSuccessJsonSchema, "Success"));
        }

        String invalidInputJsonSchema = MapUtils.getFromPath(parameterMap, "response.invalidInput", String.class);

        if (invalidInputJsonSchema != null) {
            apiResponses.put("400", createApiResponse(invalidInputJsonSchema, "Invalid input"));
        }

        String internalErrorJsonSchema = MapUtils.getFromPath(parameterMap, "response.internalError", String.class);

        if (internalErrorJsonSchema == null) {
            apiResponses.put("500", createDefaultInternalErrrorApiResponse());
        } else {
            apiResponses.put("500", createApiResponse(internalErrorJsonSchema, "Internal error"));
        }

        String forbiddenJsonSchema = MapUtils.getFromPath(parameterMap, "response.forbidden", String.class);

        if (forbiddenJsonSchema != null) {
            apiResponses.put("403", createApiResponse(forbiddenJsonSchema, "Forbidden"));
        }

        return apiResponses;
    }

    private ApiCollectionDTO toApiCollectionDTO(ApiCollection apiCollection) {
        Project project = projectService.getProjectDeploymentProject(apiCollection.getProjectDeploymentId());

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            apiCollection.getProjectDeploymentId());

        List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs = apiCollectionEndpointService.getApiEndpoints(
            apiCollection.getId())
            .stream()
            .map(this::toApiCollectionEndpointDTO)
            .toList();

        return new ApiCollectionDTO(
            apiCollection, apiCollectionEndpointDTOs, project, projectDeployment,
            tagService.getTags(apiCollection.getTagIds()));
    }

    private ApiCollectionEndpointDTO toApiCollectionEndpointDTO(ApiCollectionEndpoint apiCollectionEndpoint) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(apiCollectionEndpoint.getProjectDeploymentWorkflowId());

        String workflowUuid = projectWorkflowService.getProjectWorkflowUuid(
            projectDeploymentWorkflow.getProjectDeploymentId(), projectDeploymentWorkflow.getWorkflowId());

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpoint, projectDeploymentWorkflow.isEnabled(), workflowUuid);
    }
}

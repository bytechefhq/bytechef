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
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.automation.apiplatform.configuration.audit.ApiCollectionAuditEvents;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingInput;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class ApiCollectionFacadeImpl implements ApiCollectionFacade {

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ComponentConnectionFacade componentConnectionFacade;
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
        ComponentConnectionFacade componentConnectionFacade, EnvironmentService environmentService,
        ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService, WorkflowService workflowService) {

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.componentConnectionFacade = componentConnectionFacade;
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
    @Audited(event = ApiCollectionAuditEvents.API_COLLECTION_CREATED, data = {
        @AuditData(key = "apiCollectionId", value = "#result?.id"),
        @AuditData(key = "name", value = "#apiCollectionDTO.name"),
        @AuditData(key = "projectId", value = "#apiCollectionDTO.projectId"),
        @AuditData(key = "projectDeploymentId", value = "#result?.projectDeploymentId"),
        @AuditData(key = "environment", value = "#apiCollectionDTO.environment")
    })
    @PreAuthorize("hasResourceScopeInEnvironment(#apiCollectionDTO.projectId, 'Project', 'API_PLATFORM_CREATE', " +
        "#apiCollectionDTO.environment)")
    public ApiCollectionDTO createApiCollection(ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setDescription(apiCollection.getDescription());
        projectDeployment.setEnvironment(apiCollectionDTO.environment());
        projectDeployment.setName(apiCollectionDTO.name());
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
    @PreAuthorize("hasPermission(#apiCollectionEndpointDTO.apiCollectionId, 'ApiCollection', 'API_PLATFORM_EDIT')")
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
    @Audited(event = ApiCollectionAuditEvents.API_COLLECTION_DELETED, data = {
        @AuditData(key = "apiCollectionId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'ApiCollection', 'API_PLATFORM_DELETE')")
    public void deleteApiCollection(long id) {
        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(id)) {
            apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
        }

        apiCollectionService.delete(id);
        projectDeploymentFacade.deleteProjectDeployment(apiCollection.getProjectDeploymentId());
    }

    // Removing one endpoint changes the collection, so it takes the edit scope; deleteApiCollection removes every
    // endpoint through the service directly and needs only the delete scope.
    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')")
    public void deleteApiCollectionEndpoint(long id) {
        apiCollectionEndpointService.delete(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')")
    public ApiCollectionEndpointDTO enableApiCollectionEndpoint(long id, boolean enable) {
        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointService.getOpenApiEndpoint(id);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(apiCollectionEndpoint.getProjectDeploymentWorkflowId());

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentWorkflow.getProjectDeploymentId(), projectDeploymentWorkflow.getWorkflowId(), enable);

        return toApiCollectionEndpointDTO(apiCollectionEndpoint);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollection', 'API_PLATFORM_VIEW')")
    public ApiCollectionDTO getApiCollection(long id) {
        return toApiCollectionDTO(apiCollectionService.getApiCollection(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_VIEW')")
    public ApiCollectionEndpointDTO getApiCollectionEndpoint(long id) {
        return toApiCollectionEndpointDTO(apiCollectionEndpointService.getOpenApiEndpoint(id));
    }

    @Override
    @PreAuthorize("hasPermission(#apiCollectionId, 'ApiCollection', 'API_PLATFORM_VIEW')")
    public List<ApiCollectionEndpointDTO> getApiCollectionEndpoints(long apiCollectionId) {
        List<ApiCollectionEndpoint> apiCollectionEndpoints = new ArrayList<>(
            apiCollectionEndpointService.getApiEndpoints(apiCollectionId));

        apiCollectionEndpoints.sort(
            Comparator.comparing(ApiCollectionEndpoint::getPath)
                .thenComparing(ApiCollectionEndpoint::getHttpMethod));

        return apiCollectionEndpoints.stream()
            .map(this::toApiCollectionEndpointDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'API_PLATFORM_VIEW', #environmentId)")
    public List<ApiCollectionDTO> getApiCollections(
        long workspaceId, Long environmentId, Long projectId, Long tagId) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        return apiCollectionService.getApiCollections(workspaceId, environment, projectId, tagId)
            .stream()
            .map(this::toApiCollectionDTO)
            .toList();
    }

    // Scoped to one workspace.
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'API_PLATFORM_VIEW')")
    public List<Tag> getApiCollectionTags(long workspaceId) {
        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(workspaceId, null, null, null);

        return tagService.getTags(
            apiCollections.stream()
                .map(ApiCollection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollection', 'API_PLATFORM_VIEW')")
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'API_PLATFORM_VIEW')")
    public List<Project> getWorkspaceProjects(long workspaceId) {
        return projectService.getProjects(apiCollectionService.getApiCollectionProjectIds(workspaceId));
    }

    @Override
    @PreAuthorize("hasPermission(#apiCollectionDTO.id, 'ApiCollection', 'API_PLATFORM_EDIT')")
    public ApiCollectionDTO updateApiCollection(ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        List<Tag> tags = checkTags(apiCollectionDTO.tags());

        if (!tags.isEmpty()) {
            apiCollection.setTags(tags);
        }

        return toApiCollectionDTO(apiCollectionService.update(apiCollection));
    }

    // Keyed on the endpoint id, whose collection is read from the stored row: the update writes by id and ignores the
    // collection id in the request, so a guard on that id would check a collection the write never touches.
    @Override
    @PreAuthorize("hasPermission(#apiCollectionEndpointDTO.id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')")
    public ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO) {
        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        apiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        return toApiCollectionEndpointDTO(apiCollectionEndpoint);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')")
    public ApiCollectionEndpointDTO updateApiCollectionEndpointWorkflow(
        long id, List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs) {

        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointService.getOpenApiEndpoint(id);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(apiCollectionEndpoint.getProjectDeploymentWorkflowId());

        List<ProjectDeploymentWorkflowConnection> newConnections = connections == null ? List.of() : connections;

        projectDeploymentWorkflow.setConnections(newConnections);
        projectDeploymentWorkflow.setInputs(inputs == null ? Map.of() : inputs);

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        return toApiCollectionEndpointDTO(apiCollectionEndpoint);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollection', 'API_PLATFORM_EDIT')")
    public ApiCollectionVersionUpdateDTO updateApiCollectionProjectVersion(
        long id, int projectVersion, boolean removeMissingEndpoints) {

        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        long projectDeploymentId =
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId");

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        int currentProjectVersion = projectDeployment.getProjectVersion();
        long projectId = projectDeployment.getProjectId();

        checkProjectVersion(projectId, currentProjectVersion, projectVersion);

        Map<String, String> workflowUuidsByCurrentWorkflowId = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(
            projectId, currentProjectVersion)) {

            workflowUuidsByCurrentWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        List<ProjectWorkflow> newProjectWorkflows =
            projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        Set<String> newWorkflowUuids = newProjectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Map<Long, ProjectDeploymentWorkflow> projectDeploymentWorkflowsById = new HashMap<>();
        Map<String, ProjectDeploymentWorkflow> projectDeploymentWorkflowsByUuid = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)) {

            projectDeploymentWorkflowsById.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);

            String workflowUuid = workflowUuidsByCurrentWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflowUuid != null) {
                projectDeploymentWorkflowsByUuid.put(workflowUuid, projectDeploymentWorkflow);
            }
        }

        List<KeptEndpoint> keptEndpoints = new ArrayList<>();
        List<ApiCollectionEndpoint> removedApiCollectionEndpoints = new ArrayList<>();

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(id)) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowsById.get(
                apiCollectionEndpoint.getProjectDeploymentWorkflowId());

            String workflowUuid = projectDeploymentWorkflow == null
                ? null : workflowUuidsByCurrentWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflowUuid != null && newWorkflowUuids.contains(workflowUuid)) {
                keptEndpoints.add(new KeptEndpoint(apiCollectionEndpoint, workflowUuid));
            } else {
                removedApiCollectionEndpoints.add(apiCollectionEndpoint);
            }
        }

        List<String> removedEndpointNames = removedApiCollectionEndpoints.stream()
            .map(ApiCollectionFacadeImpl::getEndpointName)
            .toList();

        if (!removedApiCollectionEndpoints.isEmpty() && !removeMissingEndpoints) {
            throw new ConfigurationException(
                "Project version %s no longer contains the workflows of endpoints %s".formatted(
                    projectVersion, removedEndpointNames),
                Map.of("endpointNames", removedEndpointNames), ApiCollectionErrorType.ENDPOINT_WORKFLOW_REMOVED);
        }

        for (ApiCollectionEndpoint removedApiCollectionEndpoint : removedApiCollectionEndpoints) {
            deleteApiCollectionEndpoint(Objects.requireNonNull(removedApiCollectionEndpoint.getId(), "id"));
        }

        Map<String, CarriedWorkflow> carriedWorkflowsByUuid = new LinkedHashMap<>();

        for (ProjectWorkflow newProjectWorkflow : newProjectWorkflows) {
            String workflowUuid = newProjectWorkflow.getUuidAsString();

            carriedWorkflowsByUuid.put(
                workflowUuid,
                carryWorkflow(
                    projectDeploymentId, newProjectWorkflow, projectDeploymentWorkflowsByUuid.get(workflowUuid)));
        }

        projectDeployment.setProjectVersion(projectVersion);

        projectDeploymentFacade.updateProjectDeployment(
            projectDeployment,
            carriedWorkflowsByUuid.values()
                .stream()
                .map(CarriedWorkflow::projectDeploymentWorkflow)
                .toList(),
            List.of());

        List<String> disabledEndpointNames = new ArrayList<>();
        List<MissingConnection> missingConnections = new ArrayList<>();
        List<MissingInput> missingInputs = new ArrayList<>();

        for (KeptEndpoint keptEndpoint : keptEndpoints) {
            ApiCollectionEndpoint apiCollectionEndpoint = keptEndpoint.apiCollectionEndpoint();

            ProjectDeploymentWorkflow projectDeploymentWorkflow = resolveProjectDeploymentWorkflow(
                projectDeploymentId, keptEndpoint.workflowUuid());

            apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflow.getId());

            apiCollectionEndpointService.update(apiCollectionEndpoint);

            CarriedWorkflow carriedWorkflow = carriedWorkflowsByUuid.get(keptEndpoint.workflowUuid());
            String endpointName = getEndpointName(apiCollectionEndpoint);

            if (carriedWorkflow.disabled()) {
                disabledEndpointNames.add(endpointName);
            }

            for (ComponentConnection componentConnection : carriedWorkflow.missingConnectionSlots()) {
                missingConnections.add(
                    new MissingConnection(
                        endpointName, componentConnection.workflowNodeName(), componentConnection.key(),
                        componentConnection.componentName()));
            }

            for (String inputName : carriedWorkflow.missingInputNames()) {
                missingInputs.add(new MissingInput(endpointName, inputName));
            }
        }

        return new ApiCollectionVersionUpdateDTO(
            toApiCollectionDTO(apiCollectionService.getApiCollection(id)), removedEndpointNames, disabledEndpointNames,
            missingConnections, missingInputs);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiCollection', 'API_PLATFORM_EDIT')")
    public void updateApiCollectionTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        apiCollectionService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    /**
     * A malformed uuid never reaches the repository — {@code UUID.fromString} throws first — so it is normalised into
     * the same absent result as a well-formed uuid naming no workflow, and reported identically by the caller.
     */
    private Optional<String> fetchProjectWorkflowWorkflowId(long projectDeploymentId, String workflowUuid) {
        try {
            return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }

    /**
     * Find-or-create the {@link ProjectDeploymentWorkflow} row of {@code workflowUuid} within
     * {@code projectDeploymentId}. Shared by the create and the update path so an endpoint can never be pointed at a
     * workflow one path knows how to resolve and the other does not.
     */
    private ProjectDeploymentWorkflow resolveProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowUuid) {

        // fetch, not get: getProjectWorkflowWorkflowId reports a stale or foreign uuid as an untyped
        // IllegalArgumentException, which the global resolver turns into a 500. A uuid naming no workflow of this
        // deployment is bad caller input, so it is reported the way this facade reports its other bad input.
        String workflowId = fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .orElseThrow(
                () -> new ConfigurationException(
                    "Workflow '%s' does not belong to this API collection".formatted(workflowUuid),
                    ApiCollectionErrorType.WORKFLOW_NOT_FOUND));

        return projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId)
            .orElseGet(() -> {
                ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                newProjectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
                newProjectDeploymentWorkflow.setWorkflowId(workflowId);

                return projectDeploymentWorkflowService.create(newProjectDeploymentWorkflow);
            });
    }

    /**
     * Builds the new version's row for one workflow from the row it replaces. A connection is kept only where its slot
     * still exists for the same component; a workflow that was enabled but now lacks a required connection or input is
     * carried disabled, since enabling it would fail validation.
     */
    private CarriedWorkflow carryWorkflow(
        long projectDeploymentId, ProjectWorkflow newProjectWorkflow,
        @Nullable ProjectDeploymentWorkflow currentProjectDeploymentWorkflow) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(newProjectWorkflow.getWorkflowId());

        if (currentProjectDeploymentWorkflow == null) {
            return new CarriedWorkflow(projectDeploymentWorkflow, false, List.of(), List.of());
        }

        List<ComponentConnection> currentSlots = componentConnectionFacade.getWorkflowComponentConnections(
            currentProjectDeploymentWorkflow.getWorkflowId());
        List<ComponentConnection> newSlots = componentConnectionFacade.getWorkflowComponentConnections(
            newProjectWorkflow.getWorkflowId());

        List<ProjectDeploymentWorkflowConnection> carriedConnections = currentProjectDeploymentWorkflow.getConnections()
            .stream()
            .filter(connection -> {
                ComponentConnection currentSlot = findSlot(currentSlots, connection);
                ComponentConnection newSlot = findSlot(newSlots, connection);

                return currentSlot != null && newSlot != null &&
                    Objects.equals(currentSlot.componentName(), newSlot.componentName());
            })
            .toList();

        List<ComponentConnection> missingConnectionSlots = newSlots.stream()
            .filter(ComponentConnection::required)
            .filter(newSlot -> carriedConnections.stream()
                .noneMatch(connection -> isSlot(newSlot, connection)))
            .toList();

        Map<String, ?> inputs = currentProjectDeploymentWorkflow.getInputs();

        Workflow newWorkflow = workflowService.getWorkflow(newProjectWorkflow.getWorkflowId());

        List<String> missingInputNames = newWorkflow.getInputs()
            .stream()
            .filter(Workflow.Input::required)
            .map(Workflow.Input::name)
            .filter(inputName -> isBlank(inputs.get(inputName)))
            .toList();

        boolean disabled = currentProjectDeploymentWorkflow.isEnabled() &&
            (!missingConnectionSlots.isEmpty() || !missingInputNames.isEmpty());

        projectDeploymentWorkflow.setConnections(carriedConnections);
        projectDeploymentWorkflow.setEnabled(currentProjectDeploymentWorkflow.isEnabled() && !disabled);
        projectDeploymentWorkflow.setInputs(inputs);

        return new CarriedWorkflow(projectDeploymentWorkflow, disabled, missingConnectionSlots, missingInputNames);
    }

    private static String getEndpointName(ApiCollectionEndpoint apiCollectionEndpoint) {
        String name = apiCollectionEndpoint.getName();

        return name == null ? apiCollectionEndpoint.getHttpMethod() + " " + apiCollectionEndpoint.getPath() : name;
    }

    @Nullable
    private static ComponentConnection findSlot(
        List<ComponentConnection> slots, ProjectDeploymentWorkflowConnection connection) {

        return slots.stream()
            .filter(slot -> isSlot(slot, connection))
            .findFirst()
            .orElse(null);
    }

    private static boolean isBlank(@Nullable Object value) {
        return value == null || (value instanceof String string && string.isBlank());
    }

    private static boolean isSlot(ComponentConnection slot, ProjectDeploymentWorkflowConnection connection) {
        return Objects.equals(slot.workflowNodeName(), connection.getWorkflowNodeName()) &&
            Objects.equals(slot.key(), connection.getWorkflowConnectionKey());
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

    /**
     * A collection only moves forward, and only onto a version that was published: a draft can still change under the
     * endpoints that would serve it.
     */
    private void checkProjectVersion(long projectId, int currentProjectVersion, int projectVersion) {
        if (projectVersion <= currentProjectVersion) {
            throw new ConfigurationException(
                "Project version %s is not newer than the current version %s".formatted(
                    projectVersion, currentProjectVersion),
                ApiCollectionErrorType.PROJECT_VERSION_INVALID);
        }

        Project project = projectService.getProject(projectId);

        boolean published = project.getProjectVersions()
            .stream()
            .anyMatch(curProjectVersion -> curProjectVersion.getVersion() == projectVersion &&
                curProjectVersion.getStatus() == ProjectVersion.Status.PUBLISHED);

        if (!published) {
            throw new ConfigurationException(
                "Project version %s is not a published version".formatted(projectVersion),
                ApiCollectionErrorType.PROJECT_VERSION_INVALID);
        }
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
            apiCollectionEndpoint, projectDeploymentWorkflow.getConnections(), projectDeploymentWorkflow.isEnabled(),
            projectDeploymentWorkflow.getInputs(), workflowUuid);
    }

    private record CarriedWorkflow(
        ProjectDeploymentWorkflow projectDeploymentWorkflow, boolean disabled,
        List<ComponentConnection> missingConnectionSlots, List<String> missingInputNames) {
    }

    private record KeptEndpoint(ApiCollectionEndpoint apiCollectionEndpoint, String workflowUuid) {
    }
}

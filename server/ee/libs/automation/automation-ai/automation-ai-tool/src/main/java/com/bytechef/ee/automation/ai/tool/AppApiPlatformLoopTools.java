/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static com.bytechef.ee.automation.ai.tool.AppLoopTools.STATUS_ARMING_FAILED;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.STATUS_DEPLOYED;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.STATUS_REFUSED;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.STATUS_UNRESOLVED;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.getAccessDeniedMessage;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.getAgentMessage;
import static com.bytechef.ee.automation.ai.tool.AppLoopTools.getArmingMessage;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.ToolScope;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingInput;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.exception.AbstractException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;

/**
 * The management MCP tools that give an app an API Platform surface in the Apps agent loop: create an API collection
 * and its endpoints, move a collection to a newer project version, list collections, and create a data table for the
 * workflows behind them. DEVELOPMENT only, facades only, refusals returned as values, as in {@link AppLoopTools}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppApiPlatformLoopTools {

    private static final Logger log = LoggerFactory.getLogger(AppApiPlatformLoopTools.class);

    static final String STATUS_CREATED = "created";
    static final String STATUS_UPDATED = "updated";

    private static final String API_PLATFORM_COMPONENT_NAME = "apiPlatform";
    private static final String API_PLATFORM_TRIGGER_TYPE = "apiPlatform/v1/newApiRequest";
    private static final String APP_API_URL_PREFIX = "/api/app/v1/api/v";
    private static final String BIND_COMMAND = "bytechef automation app bind api-collection --api-collection-id %d";
    private static final String COLUMN_TYPES = Arrays.stream(ColumnType.values())
        .map(ColumnType::name)
        .collect(Collectors.joining(", "));
    private static final String CREATE_API_COLLECTION_PERMISSIONS =
        "API_PLATFORM_CREATE in DEVELOPMENT and DEPLOYMENT_PUSH on the project";
    private static final String CREATE_API_ENDPOINT_PERMISSIONS =
        "API_PLATFORM_VIEW and API_PLATFORM_EDIT on the API collection, CONNECTION_VIEW in DEVELOPMENT";
    private static final String HTTP_METHODS = Arrays.stream(HttpMethod.values())
        .map(HttpMethod::name)
        .collect(Collectors.joining(", "));
    private static final String NEW_API_REQUEST_OPERATION = "newApiRequest";
    private static final String UNRESOLVED_MESSAGE =
        "Nothing was created. Pass connections (one {workflowNodeName, key, connectionId} per unresolved slot, " +
            "chosen from its candidates), then call createAppApiEndpoint again.";

    private final ApiCollectionFacade apiCollectionFacade;
    private final AppFacade appFacade;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectFacade projectFacade;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI")
    public AppApiPlatformLoopTools(
        ApiCollectionFacade apiCollectionFacade, AppFacade appFacade,
        ComponentConnectionFacade componentConnectionFacade, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectFacade projectFacade, ProjectWorkflowFacade projectWorkflowFacade,
        WorkspaceDataTableFacade workspaceDataTableFacade) {

        this.apiCollectionFacade = apiCollectionFacade;
        this.appFacade = appFacade;
        this.componentConnectionFacade = componentConnectionFacade;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectFacade = projectFacade;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.workspaceDataTableFacade = workspaceDataTableFacade;
    }

    @Tool(
        description = "Create a DEVELOPMENT API collection: a project's published version served as REST under " +
            "contextPath, and enable it. Add endpoints with createAppApiEndpoint. The result carries bindCommand, " +
            "the bytechef CLI command that binds the collection to an app; an app calls an endpoint at " +
            "/api/app/v1/api/v{collectionVersion}/{contextPath}/{path}.")
    public ApiCollectionResult createAppApiCollection(
        @ToolParam(description = "The id of the project whose workflows serve the endpoints") long projectId,
        @ToolParam(description = "A published version of the project") int projectVersion,
        @ToolParam(description = "The collection name, unique in the workspace's DEVELOPMENT") String name,
        @ToolParam(description = "The URL segment of the collection, for example orders") String contextPath,
        @ToolParam(
            required = false,
            description = "The version of the API contract; defaults to 1") @Nullable Integer collectionVersion,
        @ToolParam(required = false, description = "A description of the collection") @Nullable String description,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return ApiCollectionResult.refused(toolScope.refusal());
        }

        if (StringUtils.isBlank(name) || StringUtils.isBlank(contextPath)) {
            return ApiCollectionResult.refused("name and contextPath are required");
        }

        long workspaceId = toolScope.workspaceId();

        ApiCollectionDTO apiCollectionDTO;

        try {
            ProjectDTO project = fetchProject(projectId);

            if (project == null) {
                return ApiCollectionResult.refused("Project %d does not exist".formatted(projectId));
            }

            if (!Objects.equals(project.workspaceId(), workspaceId)) {
                return ApiCollectionResult.refused(
                    "Project %d does not belong to workspace %d".formatted(projectId, workspaceId));
            }

            apiCollectionDTO = apiCollectionFacade.createApiCollection(
                new ApiCollectionDTO(
                    collectionVersion == null ? 1 : collectionVersion, contextPath, null, null, description, false,
                    List.of(), Environment.DEVELOPMENT, null, null, null, name, null, projectId, null, 0,
                    projectVersion, List.of(), 0));
        } catch (AccessDeniedException accessDeniedException) {
            return ApiCollectionResult.refused(
                getAccessDeniedMessage(accessDeniedException, CREATE_API_COLLECTION_PERMISSIONS));
        } catch (NoSuchElementException noSuchElementException) {
            log.warn("Creating an API collection of project {} failed", projectId, noSuchElementException);

            return ApiCollectionResult.refused(
                "Project %d or its version %d does not exist".formatted(projectId, projectVersion));
        } catch (AbstractException | IllegalArgumentException exception) {
            log.debug("Creating an API collection of project {} was refused", projectId, exception);

            return ApiCollectionResult.refused(getAgentMessage(exception));
        }

        long apiCollectionId = Objects.requireNonNull(apiCollectionDTO.id(), "id");

        try {
            projectDeploymentFacade.enableProjectDeployment(apiCollectionDTO.projectDeploymentId(), true);
        } catch (RuntimeException exception) {
            log.warn("Enabling API collection {} failed", apiCollectionId, exception);

            String reason = exception instanceof AccessDeniedException
                ? "the caller lacks DEPLOYMENT_EDIT on the collection's deployment" : getArmingMessage(exception);

            return new ApiCollectionResult(
                STATUS_ARMING_FAILED,
                ("arming failed: %s. Collection %d is saved and its deployment disabled; do not create it again. " +
                    "Keep adding endpoints with createAppApiEndpoint and bind the collection with bindCommand; a " +
                    "user who holds DEPLOYMENT_EDIT must enable it, and calls answer 409 until then.").formatted(
                        reason, apiCollectionId),
                apiCollectionId, apiCollectionDTO.contextPath(), apiCollectionDTO.collectionVersion(),
                BIND_COMMAND.formatted(apiCollectionId));
        }

        return new ApiCollectionResult(
            STATUS_DEPLOYED,
            "Created and enabled. Add endpoints with createAppApiEndpoint, then bind the collection with bindCommand.",
            apiCollectionId, apiCollectionDTO.contextPath(), apiCollectionDTO.collectionVersion(),
            BIND_COMMAND.formatted(apiCollectionId));
    }

    @Tool(
        description = "Add an endpoint to a DEVELOPMENT API collection and enable it. The workflow (its uuid, in the " +
            "collection's project version) must be triggered by apiPlatform/v1/newApiRequest. A connection slot of " +
            "the workflow gets the only usable connection that fits; otherwise pass connections " +
            "[{workflowNodeName, key, connectionId}] from the candidates an unresolved result lists, and nothing is " +
            "created until every required slot resolves. inputs are the workflow's deployment inputs. The result " +
            "carries url, the path an app calls.")
    public ApiEndpointResult createAppApiEndpoint(
        @ToolParam(description = "The id of the API collection") long apiCollectionId,
        @ToolParam(description = "The endpoint name, for example getOrders") String name,
        @ToolParam(description = "One of DELETE, GET, PATCH, POST, PUT") String httpMethod,
        @ToolParam(description = "The endpoint path under the collection, for example /orders") String path,
        @ToolParam(description = "The uuid of the workflow that serves the endpoint") String workflowUuid,
        @ToolParam(
            required = false,
            description = "An explicit connection per slot: workflowNodeName, key, connectionId") @Nullable List<EndpointConnectionChoice> connections,
        @ToolParam(
            required = false, description = "The workflow's deployment inputs") @Nullable Map<String, Object> inputs,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return ApiEndpointResult.refused(toolScope.refusal());
        }

        String inputProblem = getEndpointInputProblem(name, httpMethod, path, workflowUuid, connections);

        if (inputProblem != null) {
            return ApiEndpointResult.refused(inputProblem);
        }

        long workspaceId = toolScope.workspaceId();

        ApiCollectionDTO apiCollectionDTO;
        ApiCollectionEndpointDTO apiCollectionEndpointDTO;
        String url;

        try {
            apiCollectionDTO = apiCollectionFacade.getApiCollection(apiCollectionId);

            String collectionProblem = getCollectionProblem(apiCollectionDTO, workspaceId);

            if (collectionProblem != null) {
                return ApiEndpointResult.refused(collectionProblem);
            }

            url = getUrl(apiCollectionDTO.collectionVersion(), apiCollectionDTO.contextPath(), path);

            ProjectWorkflowDTO projectWorkflowDTO = findProjectWorkflow(apiCollectionDTO, workflowUuid);

            if (projectWorkflowDTO == null) {
                return ApiEndpointResult.refused(
                    "Workflow %s is not part of version %d of project %d, which collection %d serves".formatted(
                        workflowUuid, apiCollectionDTO.projectVersion(), apiCollectionDTO.projectId(),
                        apiCollectionId));
            }

            String triggerProblem = getTriggerProblem(workflowUuid, projectWorkflowDTO.getWorkflow());

            if (triggerProblem != null) {
                return ApiEndpointResult.refused(triggerProblem);
            }

            SlotResolution slotResolution = resolveSlots(
                workspaceId, projectWorkflowDTO.getId(), connections == null ? List.of() : connections);

            if (slotResolution.refusal() != null) {
                return ApiEndpointResult.refused(slotResolution.refusal());
            }

            if (!slotResolution.unresolvedConnections()
                .isEmpty()) {

                return new ApiEndpointResult(
                    STATUS_UNRESOLVED, UNRESOLVED_MESSAGE, null, false, url, slotResolution.unresolvedConnections());
            }

            apiCollectionEndpointDTO = apiCollectionFacade.createApiCollectionEndpoint(
                new ApiCollectionEndpointDTO(
                    apiCollectionId, null, null, false,
                    HttpMethod.valueOf(StringUtils.upperCase(StringUtils.trim(httpMethod), Locale.ROOT)), null,
                    null, null, name, path, 0L, 0, workflowUuid),
                slotResolution.connections(), inputs == null ? null : new HashMap<>(inputs));
        } catch (AccessDeniedException accessDeniedException) {
            return ApiEndpointResult.refused(
                getAccessDeniedMessage(accessDeniedException, CREATE_API_ENDPOINT_PERMISSIONS));
        } catch (NoSuchElementException noSuchElementException) {
            log.warn("Creating an endpoint of API collection {} failed", apiCollectionId, noSuchElementException);

            return ApiEndpointResult.refused("API collection %d does not exist".formatted(apiCollectionId));
        } catch (AbstractException | IllegalArgumentException exception) {
            log.debug("Creating an endpoint of API collection {} was refused", apiCollectionId, exception);

            return ApiEndpointResult.refused(getAgentMessage(exception));
        }

        long apiCollectionEndpointId = Objects.requireNonNull(apiCollectionEndpointDTO.id(), "id");

        try {
            apiCollectionFacade.enableApiCollectionEndpoint(apiCollectionEndpointId, true);
        } catch (RuntimeException exception) {
            log.warn("Enabling API collection endpoint {} failed", apiCollectionEndpointId, exception);

            String reason = exception instanceof AccessDeniedException
                ? "enabling it needs API_PLATFORM_EDIT on the endpoint and DEPLOYMENT_EDIT on the collection's " +
                    "deployment, and the caller lacks one of them"
                : getArmingMessage(exception);

            return new ApiEndpointResult(
                STATUS_ARMING_FAILED,
                "arming failed: %s. Endpoint %d is saved and disabled; do not call createAppApiEndpoint again for it."
                    .formatted(reason, apiCollectionEndpointId),
                apiCollectionEndpointId, false, url, List.of());
        }

        return new ApiEndpointResult(
            STATUS_DEPLOYED, "Created and enabled. An app bound to the collection calls it at url.",
            apiCollectionEndpointId, true, url, List.of());
    }

    @Tool(
        description = "Move a DEVELOPMENT API collection to a newer published version of its project, keeping its " +
            "collectionVersion and endpoints. Endpoints whose workflow the new version lacks are refused, listed in " +
            "the message, unless removeMissingEndpoints is true, which deletes them. The result lists " +
            "removedEndpointNames, disabledEndpointNames, and the missingConnections and missingInputs that disabled " +
            "them.")
    public ApiCollectionVersionResult updateAppApiCollectionVersion(
        @ToolParam(description = "The id of the API collection") long apiCollectionId,
        @ToolParam(description = "The newer published project version") int projectVersion,
        @ToolParam(
            required = false,
            description = "Delete endpoints whose workflow the new version lacks; defaults to false") @Nullable Boolean removeMissingEndpoints,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return ApiCollectionVersionResult.refused(toolScope.refusal());
        }

        ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO;

        try {
            String collectionProblem = getCollectionProblem(
                apiCollectionFacade.getApiCollection(apiCollectionId), toolScope.workspaceId());

            if (collectionProblem != null) {
                return ApiCollectionVersionResult.refused(collectionProblem);
            }

            apiCollectionVersionUpdateDTO = apiCollectionFacade.updateApiCollectionProjectVersion(
                apiCollectionId, projectVersion, Boolean.TRUE.equals(removeMissingEndpoints));
        } catch (AccessDeniedException accessDeniedException) {
            return ApiCollectionVersionResult.refused(
                getAccessDeniedMessage(
                    accessDeniedException,
                    "API_PLATFORM_VIEW and API_PLATFORM_EDIT on the API collection, DEPLOYMENT_PUSH on its project"));
        } catch (NoSuchElementException noSuchElementException) {
            log.warn("Moving API collection {} failed", apiCollectionId, noSuchElementException);

            return ApiCollectionVersionResult.refused("API collection %d does not exist".formatted(apiCollectionId));
        } catch (AbstractException abstractException) {
            log.debug("Moving API collection {} was refused", apiCollectionId, abstractException);

            String message = getAgentMessage(abstractException);

            if (isErrorType(abstractException, ApiCollectionErrorType.ENDPOINT_WORKFLOW_REMOVED)) {
                message += ". Pass removeMissingEndpoints=true to delete those endpoints.";
            }

            return ApiCollectionVersionResult.refused(message);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Moving API collection {} failed", apiCollectionId, illegalArgumentException);

            return ApiCollectionVersionResult.refused(getAgentMessage(illegalArgumentException));
        }

        String message = "Moved to project version %d.".formatted(projectVersion);

        if (!apiCollectionVersionUpdateDTO.disabledEndpointNames()
            .isEmpty()) {

            message += " Endpoints carried disabled, for lack of the connections and inputs listed: " +
                String.join(", ", apiCollectionVersionUpdateDTO.disabledEndpointNames()) + ".";
        }

        return new ApiCollectionVersionResult(
            STATUS_UPDATED, message, apiCollectionId, projectVersion,
            apiCollectionVersionUpdateDTO.removedEndpointNames(),
            apiCollectionVersionUpdateDTO.disabledEndpointNames(), apiCollectionVersionUpdateDTO.missingConnections(),
            apiCollectionVersionUpdateDTO.missingInputs());
    }

    @Tool(
        description = "List the DEVELOPMENT API collections of the workspace, optionally of one project, with their " +
            "endpoints (method, path, workflowUuid, enabled, url) and bindCommand.")
    public ApiCollectionListResult listAppApiCollections(
        @ToolParam(required = false, description = "Only collections of this project") @Nullable Long projectId,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return new ApiCollectionListResult(toolScope.refusal(), List.of());
        }

        List<ApiCollectionDTO> apiCollectionDTOs;

        try {
            apiCollectionDTOs = apiCollectionFacade.getApiCollections(
                toolScope.workspaceId(), (long) Environment.DEVELOPMENT.ordinal(), projectId, null);
        } catch (AccessDeniedException accessDeniedException) {
            return new ApiCollectionListResult(
                getAccessDeniedMessage(accessDeniedException, "API_PLATFORM_VIEW in DEVELOPMENT"), List.of());
        }

        return new ApiCollectionListResult(
            null,
            apiCollectionDTOs.stream()
                .map(AppApiPlatformLoopTools::toApiCollectionSummary)
                .toList());
    }

    @Tool(
        description = "Create an empty DEVELOPMENT data table of the workspace with an explicit column schema. " +
            "Column types: STRING, NUMBER, INTEGER, DATE, DATE_TIME, BOOLEAN. Workflow dataTable/v1/* actions take " +
            "table = the baseName the result returns.")
    public DataTableResult createAppDataTable(
        @ToolParam(description = "A snake_case identifier for the table") String baseName,
        @ToolParam(description = "The columns of the table, at least one") List<DataTableColumnInput> columns,
        @ToolParam(required = false, description = "A description of the table") @Nullable String description,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return DataTableResult.refused(toolScope.refusal());
        }

        if (StringUtils.isBlank(baseName)) {
            return DataTableResult.refused("baseName is required");
        }

        if (columns == null || columns.isEmpty()) {
            return DataTableResult.refused("columns must hold at least one {name, type}");
        }

        List<ColumnSpec> columnSpecs = new ArrayList<>();

        for (DataTableColumnInput column : columns) {
            if (column == null || StringUtils.isBlank(column.name()) || StringUtils.isBlank(column.type())) {
                return DataTableResult.refused("Every column needs a name and a type (one of %s)".formatted(
                    COLUMN_TYPES));
            }

            ColumnType columnType = findColumnType(column.type());

            if (columnType == null) {
                return DataTableResult.refused(
                    "Column %s has unsupported type %s; supported types: %s".formatted(
                        column.name(), column.type(), COLUMN_TYPES));
            }

            columnSpecs.add(new ColumnSpec(column.name(), columnType));
        }

        try {
            workspaceDataTableFacade.createTable(
                baseName, description == null ? "" : description, columnSpecs, toolScope.workspaceId(),
                Environment.DEVELOPMENT.ordinal());
        } catch (AccessDeniedException accessDeniedException) {
            return DataTableResult.refused(
                getAccessDeniedMessage(accessDeniedException, "DATA_TABLE_CREATE in DEVELOPMENT"));
        } catch (AbstractException | IllegalArgumentException exception) {
            log.debug("Creating data table {} was refused", baseName, exception);

            return DataTableResult.refused(getAgentMessage(exception));
        }

        return new DataTableResult(
            STATUS_CREATED,
            "Created. Workflow dataTable/v1/* actions take table = %s.".formatted(baseName), baseName);
    }

    private static @Nullable ColumnType findColumnType(@Nullable String type) {
        String normalizedType = StringUtils.upperCase(StringUtils.trimToEmpty(type), Locale.ROOT);

        return Arrays.stream(ColumnType.values())
            .filter(columnType -> Objects.equals(columnType.name(), normalizedType))
            .findFirst()
            .orElse(null);
    }

    private @Nullable ProjectDTO fetchProject(long projectId) {
        try {
            return projectFacade.getProject(projectId);
        } catch (NoSuchElementException noSuchElementException) {
            return null;
        }
    }

    private @Nullable ProjectWorkflowDTO findProjectWorkflow(ApiCollectionDTO apiCollectionDTO, String workflowUuid) {
        return projectWorkflowFacade
            .getProjectVersionWorkflows(apiCollectionDTO.projectId(), apiCollectionDTO.projectVersion(), false)
            .stream()
            .filter(projectWorkflowDTO -> Objects.equals(projectWorkflowDTO.getWorkflowUuid(), workflowUuid))
            .findFirst()
            .orElse(null);
    }

    private static @Nullable String getCollectionProblem(ApiCollectionDTO apiCollectionDTO, long workspaceId) {
        if (apiCollectionDTO.environment() != Environment.DEVELOPMENT) {
            return "API collection %d is in %s; the app loop changes DEVELOPMENT collections only".formatted(
                apiCollectionDTO.id(), apiCollectionDTO.environment());
        }

        Project project = apiCollectionDTO.project();

        if (project == null || !Objects.equals(project.getWorkspaceId(), workspaceId)) {
            return "API collection %d does not belong to workspace %d".formatted(apiCollectionDTO.id(), workspaceId);
        }

        return null;
    }

    private static @Nullable String getEndpointInputProblem(
        String name, String httpMethod, String path, String workflowUuid,
        @Nullable List<EndpointConnectionChoice> connections) {

        if (StringUtils.isBlank(name) || StringUtils.isBlank(path) || StringUtils.isBlank(workflowUuid)) {
            return "name, path and workflowUuid are required";
        }

        String normalizedHttpMethod = StringUtils.upperCase(StringUtils.trimToEmpty(httpMethod), Locale.ROOT);

        boolean knownHttpMethod = Arrays.stream(HttpMethod.values())
            .anyMatch(method -> Objects.equals(method.name(), normalizedHttpMethod));

        if (!knownHttpMethod) {
            return "httpMethod %s is not one of %s".formatted(httpMethod, HTTP_METHODS);
        }

        if (connections != null) {
            for (int index = 0; index < connections.size(); index++) {
                EndpointConnectionChoice endpointConnectionChoice = connections.get(index);

                if (endpointConnectionChoice == null || endpointConnectionChoice.workflowNodeName() == null ||
                    endpointConnectionChoice.key() == null || endpointConnectionChoice.connectionId() == null) {

                    return ("connections[%d] needs workflowNodeName, key and connectionId, taken from an unresolved " +
                        "slot").formatted(index);
                }
            }
        }

        return null;
    }

    private static @Nullable String getTriggerProblem(String workflowUuid, Workflow workflow) {
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (workflowTriggers.isEmpty()) {
            return "Workflow %s has no trigger; an API endpoint needs a workflow triggered by %s".formatted(
                workflowUuid, API_PLATFORM_TRIGGER_TYPE);
        }

        WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

        String triggerType = workflowTrigger.getType();

        if (isApiPlatformTrigger(triggerType)) {
            return null;
        }

        return "Workflow %s is triggered by %s; an API endpoint needs a workflow triggered by %s".formatted(
            workflowUuid, triggerType, API_PLATFORM_TRIGGER_TYPE);
    }

    static String getUrl(int collectionVersion, String contextPath, String path) {
        String trimmedContextPath = StringUtils.strip(contextPath, "/");
        String trimmedPath = StringUtils.strip(path, "/");

        String url = APP_API_URL_PREFIX + collectionVersion + "/" + trimmedContextPath;

        return StringUtils.isEmpty(trimmedPath) ? url : url + "/" + trimmedPath;
    }

    private static boolean isApiPlatformTrigger(@Nullable String triggerType) {
        if (triggerType == null) {
            return false;
        }

        String[] typeItems = triggerType.split("/");

        return typeItems.length == 3 && Objects.equals(typeItems[0], API_PLATFORM_COMPONENT_NAME) &&
            Objects.equals(typeItems[2], NEW_API_REQUEST_OPERATION);
    }

    private static boolean isErrorType(AbstractException abstractException, ApiCollectionErrorType errorType) {
        return abstractException.getErrorKey() == errorType.getErrorKey() &&
            Objects.equals(abstractException.getEntityClass(), errorType.getErrorClass());
    }

    private SlotResolution resolveSlots(
        long workspaceId, String workflowId, List<EndpointConnectionChoice> endpointConnectionChoices) {

        List<ComponentConnection> slots = componentConnectionFacade.getWorkflowComponentConnections(workflowId);

        for (EndpointConnectionChoice endpointConnectionChoice : endpointConnectionChoices) {
            boolean slotExists = slots.stream()
                .anyMatch(slot -> isSlot(slot, endpointConnectionChoice));

            if (!slotExists) {
                return SlotResolution.refused(
                    "The workflow has no connection slot %s/%s".formatted(
                        endpointConnectionChoice.workflowNodeName(), endpointConnectionChoice.key()));
            }
        }

        List<AppWorkspaceConnectionDTO> usableConnections = slots.isEmpty()
            ? List.of()
            : appFacade.getConnections(workspaceId, Environment.DEVELOPMENT, null)
                .stream()
                .filter(connection -> connection.usable() && !connection.aiProvider())
                .toList();

        List<ProjectDeploymentWorkflowConnection> connections = new ArrayList<>();
        List<UnresolvedEndpointConnection> unresolvedConnections = new ArrayList<>();

        for (ComponentConnection slot : slots) {
            List<ConnectionCandidate> candidates = usableConnections.stream()
                .filter(connection -> Objects.equals(connection.componentName(), slot.componentName()))
                .map(connection -> new ConnectionCandidate(connection.id(), connection.name()))
                .toList();

            EndpointConnectionChoice endpointConnectionChoice = endpointConnectionChoices.stream()
                .filter(choice -> isSlot(slot, choice))
                .findFirst()
                .orElse(null);

            Long connectionId = null;

            if (endpointConnectionChoice != null) {
                connectionId = endpointConnectionChoice.connectionId();

                Long chosenConnectionId = connectionId;

                boolean usable = candidates.stream()
                    .anyMatch(candidate -> Objects.equals(candidate.id(), chosenConnectionId));

                if (!usable) {
                    return SlotResolution.refused(
                        "Connection %d cannot be used for slot %s/%s; usable candidates: %s".formatted(
                            connectionId, slot.workflowNodeName(), slot.key(), describeCandidates(candidates)));
                }
            } else if (slot.required() && candidates.size() == 1) {
                ConnectionCandidate candidate = candidates.getFirst();

                connectionId = candidate.id();
            }

            if (connectionId != null) {
                connections.add(
                    new ProjectDeploymentWorkflowConnection(connectionId, slot.key(), slot.workflowNodeName()));
            } else if (slot.required()) {
                unresolvedConnections.add(
                    new UnresolvedEndpointConnection(
                        slot.workflowNodeName(), slot.key(), slot.componentName(), candidates));
            }
        }

        return new SlotResolution(connections, unresolvedConnections, null);
    }

    private static String describeCandidates(List<ConnectionCandidate> candidates) {
        if (candidates.isEmpty()) {
            return "none";
        }

        return candidates.stream()
            .map(candidate -> "%d (%s)".formatted(candidate.id(), candidate.name()))
            .collect(Collectors.joining(", "));
    }

    private static boolean isSlot(ComponentConnection slot, EndpointConnectionChoice endpointConnectionChoice) {
        return Objects.equals(slot.workflowNodeName(), endpointConnectionChoice.workflowNodeName()) &&
            Objects.equals(slot.key(), endpointConnectionChoice.key());
    }

    private static ApiCollectionSummary toApiCollectionSummary(ApiCollectionDTO apiCollectionDTO) {
        long apiCollectionId = Objects.requireNonNull(apiCollectionDTO.id(), "id");

        List<ApiEndpointSummary> apiEndpointSummaries = apiCollectionDTO.endpoints()
            .stream()
            .map(apiCollectionEndpointDTO -> new ApiEndpointSummary(
                Objects.requireNonNull(apiCollectionEndpointDTO.id(), "id"), apiCollectionEndpointDTO.name(),
                apiCollectionEndpointDTO.httpMethod()
                    .name(),
                apiCollectionEndpointDTO.path(), apiCollectionEndpointDTO.workflowUuid(),
                apiCollectionEndpointDTO.enabled(),
                getUrl(
                    apiCollectionDTO.collectionVersion(), apiCollectionDTO.contextPath(),
                    apiCollectionEndpointDTO.path())))
            .toList();

        return new ApiCollectionSummary(
            apiCollectionId, apiCollectionDTO.name(), apiCollectionDTO.projectId(), apiCollectionDTO.projectVersion(),
            apiCollectionDTO.contextPath(), apiCollectionDTO.collectionVersion(), apiCollectionDTO.enabled(),
            BIND_COMMAND.formatted(apiCollectionId), apiEndpointSummaries);
    }

    public record ApiCollectionResult(
        String status, @Nullable String message, @Nullable Long apiCollectionId, @Nullable String contextPath,
        @Nullable Integer collectionVersion, @Nullable String bindCommand) {

        static ApiCollectionResult refused(@Nullable String message) {
            return new ApiCollectionResult(STATUS_REFUSED, message, null, null, null, null);
        }
    }

    @SuppressFBWarnings("EI")
    public record ApiCollectionListResult(@Nullable String error, List<ApiCollectionSummary> apiCollections) {
    }

    @SuppressFBWarnings("EI")
    public record ApiCollectionSummary(
        long apiCollectionId, String name, long projectId, int projectVersion, String contextPath,
        int collectionVersion, boolean enabled, String bindCommand, List<ApiEndpointSummary> endpoints) {
    }

    @SuppressFBWarnings("EI")
    public record ApiCollectionVersionResult(
        String status, @Nullable String message, @Nullable Long apiCollectionId, @Nullable Integer projectVersion,
        List<String> removedEndpointNames, List<String> disabledEndpointNames,
        List<MissingConnection> missingConnections, List<MissingInput> missingInputs) {

        static ApiCollectionVersionResult refused(@Nullable String message) {
            return new ApiCollectionVersionResult(
                STATUS_REFUSED, message, null, null, List.of(), List.of(), List.of(), List.of());
        }
    }

    @SuppressFBWarnings("EI")
    public record ApiEndpointResult(
        String status, @Nullable String message, @Nullable Long apiCollectionEndpointId, boolean enabled,
        @Nullable String url, List<UnresolvedEndpointConnection> unresolvedConnections) {

        static ApiEndpointResult refused(@Nullable String message) {
            return new ApiEndpointResult(STATUS_REFUSED, message, null, false, null, List.of());
        }
    }

    public record ApiEndpointSummary(
        long apiCollectionEndpointId, String name, String method, String path, String workflowUuid, boolean enabled,
        String url) {
    }

    public record ConnectionCandidate(long id, String name) {
    }

    public record DataTableColumnInput(
        @ToolParam(required = true, description = "A snake_case column name") @Nullable String name,
        @ToolParam(
            required = true,
            description = "One of STRING, NUMBER, INTEGER, DATE, DATE_TIME, BOOLEAN") @Nullable String type) {
    }

    public record DataTableResult(String status, @Nullable String message, @Nullable String baseName) {

        static DataTableResult refused(@Nullable String message) {
            return new DataTableResult(STATUS_REFUSED, message, null);
        }
    }

    public record EndpointConnectionChoice(
        @ToolParam(required = true, description = "The workflow node of the slot") @Nullable String workflowNodeName,
        @ToolParam(required = true, description = "The connection key of the slot") @Nullable String key,
        @ToolParam(required = true, description = "The connection to put in the slot") @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record UnresolvedEndpointConnection(
        String workflowNodeName, String key, String componentName, List<ConnectionCandidate> candidates) {
    }

    @SuppressFBWarnings("EI")
    private record SlotResolution(
        List<ProjectDeploymentWorkflowConnection> connections,
        List<UnresolvedEndpointConnection> unresolvedConnections, @Nullable String refusal) {

        static SlotResolution refused(String refusal) {
            return new SlotResolution(List.of(), List.of(), refusal);
        }
    }
}

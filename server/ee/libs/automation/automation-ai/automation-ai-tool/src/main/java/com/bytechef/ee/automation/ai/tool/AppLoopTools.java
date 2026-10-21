/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.MissingInput;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.UnresolvedConnection;
import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;

/**
 * The management MCP tools of the Apps agent loop: deploy a project for an app, list the connections to choose from,
 * list what an app can call. DEVELOPMENT only; the workspace comes from {@code WorkspaceScopedFlatToolCallback}. They
 * run as the MCP key's user and go through facades only. Refusals are returned as values, never thrown: the wrapper
 * reduces a thrown exception to its class name, and the agent needs the message to correct itself.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppLoopTools {

    private static final Logger log = LoggerFactory.getLogger(AppLoopTools.class);

    static final String DEVELOPMENT_ONLY = "The app loop runs in DEVELOPMENT only";
    static final String STATUS_ARMING_FAILED = "arming failed";
    static final String STATUS_DEPLOYED = "deployed";
    static final String STATUS_REFUSED = "refused";
    static final String STATUS_UNRESOLVED = "unresolved";

    private static final String BIND_COMMAND =
        "bytechef automation app bind workflow --workflow-key %s --deployment-id %d --workflow-uuid %s";
    private static final String DEPLOY_PROJECT_PERMISSIONS =
        "WORKFLOW_EDIT on the project in DEVELOPMENT, DEPLOYMENT_VIEW in DEVELOPMENT, DEPLOYMENT_EDIT on the " +
            "project's DEVELOPMENT deployment, CONNECTION_VIEW in DEVELOPMENT";
    private static final Pattern EDGE_UNDERSCORES = Pattern.compile("^_+|_+$");
    private static final Pattern NON_KEY_CHARACTERS = Pattern.compile("[^a-z0-9]+");
    private static final String UNRESOLVED_MESSAGE =
        "Nothing was written. Pass connections (one {workflowUuid, workflowNodeName, key, connectionId} per " +
            "unresolved slot, chosen from its candidates) and inputs for the missing ones, then call deployProject " +
            "again.";
    private static final int WORKFLOW_KEY_MAX_LENGTH = 64;

    private final AppFacade appFacade;
    private final AppWorkflowFacade appWorkflowFacade;
    private final CallableWorkflowChecker callableWorkflowChecker;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectFacade projectFacade;
    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AppLoopTools(
        AppFacade appFacade, AppWorkflowFacade appWorkflowFacade, CallableWorkflowChecker callableWorkflowChecker,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectFacade projectFacade,
        ProjectWorkflowFacade projectWorkflowFacade) {

        this.appFacade = appFacade;
        this.appWorkflowFacade = appWorkflowFacade;
        this.callableWorkflowChecker = callableWorkflowChecker;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectFacade = projectFacade;
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @Tool(
        description = "Deploy a project's latest published version to its single DEVELOPMENT deployment (created " +
            "the first time, updated in place after that, so app bindings stay valid) and enable it. Every workflow " +
            "of the version gets a deployment row: enabledWorkflows (workflow uuids) are enabled, the others keep " +
            "their state, and when enabledWorkflows is omitted the app-callable workflows are enabled. A connection " +
            "slot gets the only usable connection that fits; otherwise pass connections " +
            "[{workflowUuid, workflowNodeName, key, connectionId}] from the candidates the result lists. inputs " +
            "maps a workflow uuid to its deployment inputs. Call publishProject first. Each enabled app-callable " +
            "workflow in the result carries bindCommand, the bytechef CLI command that binds it to an app, and " +
            "every workflow carries draftWorkflowId, the id to pass to buildWorkflow for the next change.")
    public DeployProjectResult deployProject(
        @ToolParam(description = "The id of the project to deploy") long projectId,
        @ToolParam(
            required = false,
            description = "The DEVELOPMENT deployment to update; needed only when the project has several") @Nullable Long projectDeploymentId,
        @ToolParam(
            required = false,
            description = "Workflow uuids to enable; omitted, the app-callable workflows are enabled") @Nullable List<String> enabledWorkflows,
        @ToolParam(
            required = false,
            description = "An explicit connection per slot: workflowUuid, workflowNodeName, key, connectionId") @Nullable List<ConnectionChoice> connections,
        @ToolParam(
            required = false,
            description = "Deployment inputs per workflow uuid") @Nullable Map<String, Map<String, Object>> inputs,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return DeployProjectResult.refused(toolScope.refusal(), List.of());
        }

        String inputProblem = getInputProblem(enabledWorkflows, connections, inputs);

        if (inputProblem != null) {
            return DeployProjectResult.refused(inputProblem, List.of());
        }

        long workspaceId = toolScope.workspaceId();

        DeployProjectResultDTO deployProjectResultDTO;

        try {
            ProjectDTO project = fetchProject(projectId);

            if (project == null) {
                return DeployProjectResult.refused("Project %d does not exist".formatted(projectId), List.of());
            }

            if (!Objects.equals(project.workspaceId(), workspaceId)) {
                return DeployProjectResult.refused(
                    "Project %d does not belong to workspace %d".formatted(projectId, workspaceId), List.of());
            }

            Long targetProjectDeploymentId = projectDeploymentId;

            if (targetProjectDeploymentId == null) {
                List<DeploymentChoice> deployments = getDevelopmentDeployments(workspaceId, projectId);

                if (deployments.size() > 1) {
                    return DeployProjectResult.refused(
                        "Project %d has several DEVELOPMENT deployments; pass projectDeploymentId".formatted(
                            projectId),
                        deployments);
                }

                if (deployments.size() == 1) {
                    DeploymentChoice deploymentChoice = deployments.getFirst();

                    targetProjectDeploymentId = deploymentChoice.projectDeploymentId();
                }
            }

            deployProjectResultDTO = projectDeploymentFacade.deployProject(
                projectId, targetProjectDeploymentId,
                enabledWorkflows == null ? getAppCallableWorkflowUuids(project) : enabledWorkflows,
                toConnectionChoiceDTOs(connections), toInputs(inputs));
        } catch (AccessDeniedException accessDeniedException) {
            return DeployProjectResult.refused(
                getAccessDeniedMessage(accessDeniedException, DEPLOY_PROJECT_PERMISSIONS), List.of());
        } catch (NoSuchElementException noSuchElementException) {
            log.warn("Deploying project {} failed on a missing element", projectId, noSuchElementException);

            return DeployProjectResult.refused(
                "Project %d, or the deployment named, does not exist or cannot be deployed".formatted(projectId),
                List.of());
        } catch (AbstractException abstractException) {
            log.debug("Deploying project {} was refused", projectId, abstractException);

            return DeployProjectResult.refused(getAgentMessage(abstractException), List.of());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Deploying project {} failed", projectId, illegalArgumentException);

            return DeployProjectResult.refused(getAgentMessage(illegalArgumentException), List.of());
        }

        if (!deployProjectResultDTO.written()) {
            return toResult(STATUS_UNRESOLVED, UNRESOLVED_MESSAGE, deployProjectResultDTO, false);
        }

        long savedProjectDeploymentId = Objects.requireNonNull(deployProjectResultDTO.projectDeploymentId());

        try {
            projectDeploymentFacade.enableProjectDeployment(savedProjectDeploymentId, true);
        } catch (AccessDeniedException accessDeniedException) {
            log.warn("Arming deployment {} failed: {}", savedProjectDeploymentId, accessDeniedException.toString());

            return toResult(
                STATUS_ARMING_FAILED,
                ("arming failed: the caller lacks DEPLOYMENT_EDIT on deployment %d. The configuration is saved and " +
                    "the deployment is disabled; a user who holds DEPLOYMENT_EDIT must enable it.").formatted(
                        savedProjectDeploymentId),
                deployProjectResultDTO, false);
        } catch (RuntimeException exception) {
            log.warn("Arming deployment {} failed", savedProjectDeploymentId, exception);

            return toResult(
                STATUS_ARMING_FAILED,
                ("arming failed: %s. The configuration is saved and deployment %d is disabled; call deployProject " +
                    "again to retry.").formatted(getArmingMessage(exception), savedProjectDeploymentId),
                deployProjectResultDTO, false);
        }

        return toResult(STATUS_DEPLOYED, getDeployedMessage(deployProjectResultDTO), deployProjectResultDTO, true);
    }

    private @Nullable ProjectDTO fetchProject(long projectId) {
        try {
            return projectFacade.getProject(projectId);
        } catch (NoSuchElementException noSuchElementException) {
            return null;
        }
    }

    @Tool(
        description = "List the DEVELOPMENT connections of the workspace: id, name, componentName, " +
            "connectionVersion, authorizationType, credentialStatus, aiProvider and usable (valid credentials and " +
            "the caller may use it). Never parameters or credentials. Use it to choose connectionId values for " +
            "deployProject and for app connection bindings; AI-provider connections never fill a deployment slot.")
    public ConnectionListResult listConnections(
        @ToolParam(
            required = false,
            description = "Only connections of this component, for example slack") @Nullable String componentName,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return new ConnectionListResult(toolScope.refusal(), List.of());
        }

        try {
            return new ConnectionListResult(
                null, appFacade.getConnections(toolScope.workspaceId(), Environment.DEVELOPMENT, componentName));
        } catch (AccessDeniedException accessDeniedException) {
            return new ConnectionListResult(
                getAccessDeniedMessage(accessDeniedException, "CONNECTION_VIEW in DEVELOPMENT"), List.of());
        }
    }

    @Tool(
        description = "List the workflows an app can call in DEVELOPMENT: enabled Call-workflow workflows of " +
            "enabled deployments of the workspace, optionally of one project. Each: projectDeploymentId, " +
            "workflowUuid, workflowName, projectId, projectName, inputSchema. Bind one with " +
            "bytechef automation app bind workflow --workflow-key <KEY> --deployment-id <projectDeploymentId> " +
            "--workflow-uuid <workflowUuid>.")
    public CallableWorkflowListResult listAppCallableWorkflows(
        @ToolParam(required = false, description = "Only workflows of this project") @Nullable Long projectId,
        ToolContext toolContext) {

        ToolScope toolScope = ToolScope.of(toolContext);

        if (toolScope.refusal() != null) {
            return new CallableWorkflowListResult(toolScope.refusal(), List.of());
        }

        try {
            return new CallableWorkflowListResult(
                null, appWorkflowFacade.getCallableWorkflows(toolScope.workspaceId(), projectId));
        } catch (AccessDeniedException accessDeniedException) {
            return new CallableWorkflowListResult(
                getAccessDeniedMessage(accessDeniedException, "DEPLOYMENT_VIEW in DEVELOPMENT"), List.of());
        }
    }

    static String suggestWorkflowKey(String workflowName) {
        String lowerCaseName = workflowName.toLowerCase(Locale.ROOT);

        Matcher nonKeyCharactersMatcher = NON_KEY_CHARACTERS.matcher(lowerCaseName);

        String underscored = nonKeyCharactersMatcher.replaceAll("_");

        Matcher edgeUnderscoresMatcher = EDGE_UNDERSCORES.matcher(underscored);

        String workflowKey = edgeUnderscoresMatcher.replaceAll("");

        if (workflowKey.isEmpty()) {
            return "workflow";
        }

        if (!Character.isLetter(workflowKey.charAt(0))) {
            workflowKey = "workflow_" + workflowKey;
        }

        return workflowKey.length() > WORKFLOW_KEY_MAX_LENGTH
            ? workflowKey.substring(0, WORKFLOW_KEY_MAX_LENGTH) : workflowKey;
    }

    static String getAccessDeniedMessage(AccessDeniedException accessDeniedException, String permissions) {
        return "%s: the caller lacks a permission this tool needs (%s)".formatted(
            accessDeniedException.getMessage(), permissions);
    }

    private List<String> getAppCallableWorkflowUuids(ProjectDTO project) {
        Optional<ProjectVersion> lastPublishedProjectVersion = project.projectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
            .max(Comparator.comparingInt(ProjectVersion::getVersion));

        if (lastPublishedProjectVersion.isEmpty()) {
            return List.of();
        }

        ProjectVersion projectVersion = lastPublishedProjectVersion.get();

        return projectWorkflowFacade
            .getProjectVersionWorkflows(Objects.requireNonNull(project.id()), projectVersion.getVersion(), false)
            .stream()
            .filter(projectWorkflowDTO -> isAppCallable(projectWorkflowDTO.getId()))
            .map(ProjectWorkflowDTO::getWorkflowUuid)
            .toList();
    }

    private static String getDeployedMessage(DeployProjectResultDTO deployProjectResultDTO) {
        String message = "Deployed and enabled. Bind each app-callable workflow with its bindCommand; pass " +
            "draftWorkflowId to buildWorkflow for the next change.";

        if (deployProjectResultDTO.removedWorkflows()
            .isEmpty()) {

            return message;
        }

        String removedWorkflows = deployProjectResultDTO.removedWorkflows()
            .stream()
            .map(removedWorkflow -> "%s (%s)".formatted(removedWorkflow.name(), removedWorkflow.workflowUuid()))
            .collect(Collectors.joining(", "));

        return message + " Workflows removed since the previous deployment: " + removedWorkflows +
            ". App bindings to them are now unavailable; check `bytechef automation app status`.";
    }

    /**
     * Arming runs component trigger code, whose exceptions can carry third-party response text or request URLs, so only
     * a ByteChef-authored {@link ConfigurationException} passes its message through; anything else is reported by class
     * and left to the log.
     */
    static String getArmingMessage(RuntimeException exception) {
        if (exception instanceof ConfigurationException) {
            return getAgentMessage(exception);
        }

        Class<? extends RuntimeException> exceptionClass = exception.getClass();

        return exceptionClass.getSimpleName();
    }

    static String getAgentMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (exception instanceof AbstractException && message != null && !message.isBlank()) {
            return message;
        }

        Class<? extends RuntimeException> exceptionClass = exception.getClass();

        return exceptionClass.getSimpleName();
    }

    private static @Nullable String getInputProblem(
        @Nullable List<String> enabledWorkflows, @Nullable List<ConnectionChoice> connections,
        @Nullable Map<String, Map<String, Object>> inputs) {

        if (enabledWorkflows != null && enabledWorkflows.stream()
            .anyMatch(Objects::isNull)) {
            return "enabledWorkflows holds a null entry; pass workflow uuids only";
        }

        if (connections != null) {
            for (int index = 0; index < connections.size(); index++) {
                ConnectionChoice connectionChoice = connections.get(index);

                if (connectionChoice == null || connectionChoice.workflowUuid() == null ||
                    connectionChoice.workflowNodeName() == null || connectionChoice.key() == null ||
                    connectionChoice.connectionId() == null) {

                    return ("connections[%d] needs workflowUuid, workflowNodeName, key and connectionId, taken from " +
                        "an unresolved slot or a deployed connection").formatted(index);
                }
            }
        }

        if (inputs != null) {
            for (Map.Entry<String, Map<String, Object>> entry : inputs.entrySet()) {
                if (entry.getValue() == null) {
                    return "inputs of workflow %s must be an object of input names to values".formatted(
                        entry.getKey());
                }
            }
        }

        return null;
    }

    private List<DeploymentChoice> getDevelopmentDeployments(long workspaceId, long projectId) {
        return projectDeploymentFacade
            .getWorkspaceProjectDeployments(
                workspaceId, (long) Environment.DEVELOPMENT.ordinal(), projectId, null, false)
            .stream()
            .filter(projectDeploymentDTO -> !SystemProjects.isSystemDeploymentName(projectDeploymentDTO.name()))
            .map(projectDeploymentDTO -> new DeploymentChoice(
                Objects.requireNonNull(projectDeploymentDTO.id()), projectDeploymentDTO.name()))
            .toList();
    }

    private boolean isAppCallable(String workflowId) {
        CallableWorkflowCheck callableWorkflowCheck = callableWorkflowChecker.check(workflowId);

        return callableWorkflowCheck.callable();
    }

    private static List<DeploymentConnectionChoiceDTO> toConnectionChoiceDTOs(
        @Nullable List<ConnectionChoice> connections) {

        if (connections == null) {
            return List.of();
        }

        return connections.stream()
            .map(connectionChoice -> new DeploymentConnectionChoiceDTO(
                connectionChoice.workflowUuid(), connectionChoice.workflowNodeName(), connectionChoice.key(),
                Objects.requireNonNull(connectionChoice.connectionId())))
            .toList();
    }

    private static Map<String, Map<String, ?>> toInputs(@Nullable Map<String, Map<String, Object>> inputs) {
        if (inputs == null) {
            return Map.of();
        }

        return new HashMap<>(inputs);
    }

    private DeployProjectResult toResult(
        String status, String message, DeployProjectResultDTO deployProjectResultDTO, boolean enabled) {

        List<DeployedWorkflow> deployedWorkflows = new ArrayList<>();

        for (DeployProjectResultDTO.DeployedWorkflow deployedWorkflow : deployProjectResultDTO.workflows()) {
            CallableWorkflowCheck callableWorkflowCheck = callableWorkflowChecker.check(deployedWorkflow.workflowId());

            String bindCommand = null;

            if (enabled && deployedWorkflow.enabled() && callableWorkflowCheck.callable()) {
                bindCommand = BIND_COMMAND.formatted(
                    suggestWorkflowKey(deployedWorkflow.name()), deployProjectResultDTO.projectDeploymentId(),
                    deployedWorkflow.workflowUuid());
            }

            deployedWorkflows.add(
                new DeployedWorkflow(
                    deployedWorkflow.workflowUuid(), deployedWorkflow.name(), deployedWorkflow.enabled(),
                    callableWorkflowCheck.callable(), callableWorkflowCheck.inputSchema(),
                    deployedWorkflow.draftWorkflowId(), deployedWorkflow.connections(), bindCommand));
        }

        return new DeployProjectResult(
            status, message, deployProjectResultDTO.projectDeploymentId(), deployProjectResultDTO.projectVersion(),
            enabled, deployedWorkflows, deployProjectResultDTO.removedWorkflows(),
            deployProjectResultDTO.unresolvedConnections(), deployProjectResultDTO.missingInputs(), List.of());
    }

    public record ConnectionChoice(
        @ToolParam(
            required = true,
            description = "The uuid of the workflow the slot belongs to") @Nullable String workflowUuid,
        @ToolParam(required = true, description = "The workflow node of the slot") @Nullable String workflowNodeName,
        @ToolParam(required = true, description = "The connection key of the slot") @Nullable String key,
        @ToolParam(required = true, description = "The connection to put in the slot") @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record CallableWorkflowListResult(@Nullable String error, List<AppCallableWorkflowDTO> workflows) {
    }

    @SuppressFBWarnings("EI")
    public record ConnectionListResult(@Nullable String error, List<AppWorkspaceConnectionDTO> connections) {
    }

    @SuppressFBWarnings("EI")
    public record DeployedWorkflow(
        String workflowUuid, String name, boolean enabled, boolean appCallable, @Nullable String inputSchema,
        @Nullable String draftWorkflowId, List<DeployedConnection> connections, @Nullable String bindCommand) {
    }

    public record DeploymentChoice(long projectDeploymentId, String name) {
    }

    @SuppressFBWarnings("EI")
    public record DeployProjectResult(
        String status, @Nullable String message, @Nullable Long projectDeploymentId,
        @Nullable Integer projectVersion, boolean enabled, List<DeployedWorkflow> workflows,
        List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
        List<MissingInput> missingInputs, List<DeploymentChoice> deployments) {

        static DeployProjectResult refused(@Nullable String message, List<DeploymentChoice> deployments) {
            return new DeployProjectResult(
                STATUS_REFUSED, message, null, null, false, List.of(), List.of(), List.of(), List.of(), deployments);
        }
    }

    record ToolScope(long workspaceId, @Nullable String refusal) {

        static ToolScope of(@Nullable ToolContext toolContext) {
            AutomationToolInvocationContext automationToolInvocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = automationToolInvocationContext == null
                ? null : automationToolInvocationContext.workspaceId();

            if (workspaceId == null) {
                return new ToolScope(0, "Workspace context unavailable; pass workspaceId");
            }

            int environmentOrdinal = AutomationToolInvocationContext.resolveEnvironmentOrDefault(
                automationToolInvocationContext);

            if (environmentOrdinal != Environment.DEVELOPMENT.ordinal()) {
                return new ToolScope(workspaceId, DEVELOPMENT_ONLY);
            }

            return new ToolScope(workspaceId, null);
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserAccessDeciderImpl implements ConnectedUserAccessDecider {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserAccessDeciderImpl.class);

    private static final Set<String> DEPLOYMENT_TYPES = Set.of("ProjectDeployment", "ProjectDeploymentWorkflow");

    private static final Set<String> PROJECT_BEARING_TYPES = Set.of(
        "Project", "ProjectWorkflow", "ProjectDeployment", "ProjectDeploymentWorkflow", "Job", "TestJob",
        "TriggerExecution");

    private static final String API_PLATFORM_SCOPE_PREFIX = "API_PLATFORM_";
    private static final String CONNECTION = "Connection";
    private static final String DEPLOYMENT_SCOPE_PREFIX = "DEPLOYMENT_";
    private static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final IntegrationInstanceService integrationInstanceService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final Map<String, ResourceEnvironmentResolver> resourceEnvironmentResolvers;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    @SuppressFBWarnings("EI")
    public ConnectedUserAccessDeciderImpl(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectedUserConnectionService connectedUserConnectionService,
        ConnectedUserProjectService connectedUserProjectService, IntegrationInstanceService integrationInstanceService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.integrationInstanceService = integrationInstanceService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.resourceEnvironmentResolvers = resourceEnvironmentResolvers.stream()
            .collect(Collectors.toMap(ResourceEnvironmentResolver::resourceType, Function.identity()));
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
    }

    @Override
    public Decision decide(Serializable id, String resourceType, String scope) {
        Optional<ConnectedUserAuthentication> connectedUser = ConnectedUserAuthentications.fetchCurrent();

        if (connectedUser.isEmpty()) {
            return Decision.NOT_GOVERNED;
        }

        Decision decision;

        try {
            decision = decideForConnectedUser(connectedUser.get(), id, resourceType, scope);
        } catch (RuntimeException exception) {
            log.error("Denying {} {} for a connected user: the ownership lookup failed", resourceType, id, exception);

            decision = Decision.DENY;
        }

        logDecision(connectedUser.get(), resourceType, id, scope, decision);

        return decision;
    }

    @Override
    public Decision decideInEnvironment(Serializable id, String resourceType, String scope, Environment environment) {
        Optional<ConnectedUserAuthentication> connectedUser = ConnectedUserAuthentications.fetchCurrent();

        if (connectedUser.isEmpty()) {
            return Decision.NOT_GOVERNED;
        }

        ConnectedUserAuthentication connectedUserAuthentication = connectedUser.get();

        if (scope.startsWith(DEPLOYMENT_SCOPE_PREFIX)
            && !isPrincipalEnvironment(connectedUserAuthentication, environment)) {

            logDecision(connectedUserAuthentication, resourceType, id, scope, Decision.DENY);

            return Decision.DENY;
        }

        return decide(id, resourceType, scope);
    }

    @Override
    public Decision decideWorkflow(String workflowId, String scope) {
        Optional<ConnectedUserAuthentication> connectedUser = ConnectedUserAuthentications.fetchCurrent();

        if (connectedUser.isEmpty()) {
            return Decision.NOT_GOVERNED;
        }

        Decision decision = decideWorkflowForConnectedUser(connectedUser.get(), workflowId, scope);

        logDecision(connectedUser.get(), "Workflow", workflowId, scope, decision);

        return decision;
    }

    @Override
    public Decision decideWorkspace(long workspaceId, String scope) {
        return ConnectedUserAuthentications.fetchCurrent()
            .map(connectedUser -> Decision.DENY)
            .orElse(Decision.NOT_GOVERNED);
    }

    private Decision decideWorkflowForConnectedUser(
        ConnectedUserAuthentication connectedUser, String workflowId, String scope) {

        if (scope.startsWith(API_PLATFORM_SCOPE_PREFIX)) {
            return Decision.DENY;
        }

        try {
            Optional<Project> project = projectService.fetchWorkflowProject(workflowId);

            if (project.isEmpty()) {
                return Decision.DENY;
            }

            Project workflowProject = project.get();

            long projectId = workflowProject.getId();

            if (isOwnProject(connectedUser, projectId)) {
                return Decision.GRANT;
            }

            if (WORKFLOW_VIEW.equals(scope)
                && isPublishedTemplateWorkflow(connectedUser, projectId, workflowId)) {
                return Decision.GRANT;
            }

            return Decision.DENY;
        } catch (RuntimeException exception) {
            log.error("Denying workflow {} for a connected user: the ownership lookup failed", workflowId, exception);

            return Decision.DENY;
        }
    }

    private Decision decideForConnectedUser(
        ConnectedUserAuthentication connectedUser, Serializable id, String resourceType, String scope) {

        if (scope.startsWith(API_PLATFORM_SCOPE_PREFIX)) {
            return Decision.DENY;
        }

        if (CONNECTION.equals(resourceType)) {
            return ownsConnection(connectedUser, id) ? Decision.GRANT : Decision.DENY;
        }

        if (!PROJECT_BEARING_TYPES.contains(resourceType)) {
            return Decision.DENY;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return Decision.DENY;
        }

        OptionalLong projectId = resourceOwnershipResolver.resolveProjectId(id);

        if (projectId.isEmpty()) {
            return Decision.DENY;
        }

        if (!isOwnProject(connectedUser, projectId.getAsLong())) {
            return Decision.DENY;
        }

        if (DEPLOYMENT_TYPES.contains(resourceType) && !isInPrincipalEnvironment(connectedUser, id, resourceType)) {
            return Decision.DENY;
        }

        return Decision.GRANT;
    }

    private boolean isInPrincipalEnvironment(
        ConnectedUserAuthentication connectedUser, Serializable id, String resourceType) {

        ResourceEnvironmentResolver resourceEnvironmentResolver = resourceEnvironmentResolvers.get(resourceType);

        if (resourceEnvironmentResolver == null) {
            return false;
        }

        return resourceEnvironmentResolver.fetchEnvironment(id)
            .map(environment -> isPrincipalEnvironment(connectedUser, environment))
            .orElse(false);
    }

    private boolean isOwnProject(ConnectedUserAuthentication connectedUser, long projectId) {
        return connectedUserProjectService
            .fetchConnectUserProject(connectedUser.externalUserId(), toEnvironment(connectedUser))
            .map(connectedUserProject -> Objects.equals(connectedUserProject.getProjectId(), projectId))
            .orElse(false);
    }

    private boolean isPublishedTemplateWorkflow(
        ConnectedUserAuthentication connectedUser, long projectId, String workflowId) {

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        String workflowUuid = projectWorkflow.getUuidAsString();

        List<AutomationWorkflowProjectDTO> publishedProjects = automationWorkflowProjectFacade.getPublishedProjects(
            connectedUser.externalUserId(), toEnvironment(connectedUser));

        return publishedProjects.stream()
            .filter(publishedProject -> publishedProject.id() == projectId)
            .filter(publishedProject -> Objects.equals(
                publishedProject.lastPublishedVersion(), projectWorkflow.getProjectVersion()))
            .flatMap(publishedProject -> CollectionUtils.stream(publishedProject.workflowTemplates()))
            .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid));
    }

    private boolean ownsConnection(ConnectedUserAuthentication connectedUser, Serializable id) {
        if (!(id instanceof Number number)) {
            return false;
        }

        long connectionId = number.longValue();

        List<Long> connectionIds = connectedUserConnectionService.getConnectionIds(connectedUser.connectedUserId());

        if (connectionIds.contains(connectionId)) {
            return true;
        }

        return integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUser.connectedUserId(), toEnvironment(connectedUser))
            .stream()
            .anyMatch(integrationInstance -> integrationInstance.getConnectionId() == connectionId);
    }

    private static boolean isPrincipalEnvironment(
        ConnectedUserAuthentication connectedUser, Environment environment) {

        return environment != null && connectedUser.environmentId() == environment.ordinal();
    }

    private static Environment toEnvironment(ConnectedUserAuthentication connectedUser) {
        Environment[] environments = Environment.values();

        return environments[Math.toIntExact(connectedUser.environmentId())];
    }

    private static void logDecision(
        ConnectedUserAuthentication connectedUser, String resourceType, Serializable id, String scope,
        Decision decision) {

        log.debug(
            "Connected user {} in environment {}: {} {} {} -> {}", connectedUser.connectedUserId(),
            connectedUser.environmentId(), resourceType, id, scope, decision);
    }
}

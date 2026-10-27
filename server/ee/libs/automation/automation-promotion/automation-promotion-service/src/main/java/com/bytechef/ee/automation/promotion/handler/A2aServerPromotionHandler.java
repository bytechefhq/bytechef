/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.handler.ProjectWorkflowMappingReconciler.Mapping;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.ServerProjectStore;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.SourceProjectView;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.TargetProject;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("a2aServerPromotionHandler")
@ConditionalOnEEVersion
public class A2aServerPromotionHandler implements EnvironmentPromotionHandler {

    private static final String SERVER_KIND = "A2A server";

    private record SourceProject(
        ProjectDeployment projectDeployment, List<A2aProjectWorkflow> a2aProjectWorkflows) {
    }

    private final A2aProjectFacade a2aProjectFacade;
    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ServerProjectPromoter serverProjectPromoter;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public A2aServerPromotionHandler(
        A2aProjectFacade a2aProjectFacade, A2aProjectService a2aProjectService,
        A2aProjectWorkflowService a2aProjectWorkflowService, A2aServerService a2aServerService,
        ApplicationProperties applicationProperties, ConnectionEnvironmentMapper connectionEnvironmentMapper,
        ConnectionService connectionService, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ServerProjectPromoter serverProjectPromoter) {

        this.a2aProjectFacade = a2aProjectFacade;
        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.serverProjectPromoter = serverProjectPromoter;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.A2A_SERVER;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        A2aServer source = loadSource(sourceId, targetEnvironment);

        List<SourceProject> sourceProjects = sourceProjects(source);
        long workspaceId = workspaceId(sourceProjects);
        Optional<A2aServer> existingA2aServer = findTarget(source, targetEnvironment);
        List<String> warnings = new ArrayList<>();

        ServerProjectPromoter.addAmbiguousSourceProjectWarnings(
            warnings, sourceProjectDeployments(sourceProjects), SERVER_KIND);

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId;

        if (existingA2aServer.isPresent()) {
            targetProjectsByProjectId = targetProjects(existingA2aServer.get());

            ServerProjectPromoter.addAmbiguousTargetProjectWarnings(
                warnings, targetProjectsByProjectId, SERVER_KIND);
        } else {
            targetProjectsByProjectId = new LinkedHashMap<>();

            warnings.add("The promoted server is created disabled; enable it after reviewing its connections.");
            warnings.add("A new server URL and secret key will be generated for the promoted server.");
        }

        List<PromotionProjectPreview> projects = new ArrayList<>();
        List<SourceBinding> sourceBindings = new ArrayList<>();
        Map<Long, Long> existingTargetBindings = new LinkedHashMap<>();

        for (SourceProject sourceProject : sourceProjects) {
            ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

            long projectId = ServerProjectPromoter.getProjectId(sourceProjectDeployment);

            sourceBindings.addAll(projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment));

            TargetProject targetProject =
                ServerProjectPromoter.pollTargetProject(targetProjectsByProjectId, projectId);

            Integer targetProjectVersion = null;

            if (targetProject != null) {
                ProjectDeployment targetProjectDeployment = targetProject.projectDeployment();

                targetProjectVersion = targetProjectDeployment.getProjectVersion();

                ServerProjectPromoter.putAllIfAbsent(
                    existingTargetBindings,
                    projectDeploymentPromoter.existingTargetBindings(
                        sourceProjectDeployment, targetProjectDeployment));
            }

            Project project = projectService.getProject(projectId);

            projects.add(
                new PromotionProjectPreview(
                    projectId, project.getName(), sourceProjectDeployment.getProjectVersion(), targetProjectVersion));
        }

        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);
        Map<Long, Long> suggestedMappings =
            connectionEnvironmentMapper.suggest(workspaceId, sourceConnectionIds, targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.A2A_SERVER, sourceId, source.getEnvironment(), targetEnvironment,
            existingA2aServer.map(A2aServer::getId)
                .orElse(null),
            existingA2aServer.map(A2aServer::getName)
                .orElse(null),
            projects,
            PromotionPreviews.connectionMappings(sourceBindings, existingTargetBindings, suggestedMappings,
                connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        A2aServer source = loadSource(sourceId, targetEnvironment);

        List<SourceProject> sourceProjects = sourceProjects(source);
        long workspaceId = workspaceId(sourceProjects);

        for (SourceProject sourceProject : sourceProjects) {
            ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

            projectDeploymentPromoter.validatePromotable(
                ServerProjectPromoter.getProjectId(sourceProjectDeployment),
                sourceProjectDeployment.getProjectVersion());
        }

        List<SourceBinding> sourceBindings = new ArrayList<>();

        for (SourceProject sourceProject : sourceProjects) {
            sourceBindings.addAll(projectDeploymentPromoter.collectSourceBindings(sourceProject.projectDeployment()));
        }

        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<A2aServer> existingA2aServer = findTarget(source, targetEnvironment);
        boolean created = existingA2aServer.isEmpty();
        A2aServer target = existingA2aServer.orElseGet(() -> createTargetServer(source, targetEnvironment));

        long targetId = Objects.requireNonNull(target.getId(), "id");

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId =
            created ? new LinkedHashMap<>() : targetProjects(target);
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();

        for (SourceProject sourceProject : sourceProjects) {
            unresolvedConnectionIds.addAll(
                serverProjectPromoter.promoteProject(
                    sourceProjectView(sourceProject), targetId, targetProjectsByProjectId, requestedMappings,
                    suggestedMappings, serverProjectStore()));
        }

        for (Deque<TargetProject> staleTargetProjects : targetProjectsByProjectId.values()) {
            for (TargetProject staleTargetProject : staleTargetProjects) {
                a2aProjectFacade.deleteA2aProject(staleTargetProject.id());
            }
        }

        String targetUrl =
            publicUrl + "/api/automation/a2a/" + target.getSecretKey() + "/.well-known/agent-card.json";

        return new EnvironmentPromotionResult(targetId, created, targetUrl, List.copyOf(unresolvedConnectionIds));
    }

    private A2aServer createTargetServer(A2aServer source, Environment targetEnvironment) {
        A2aServer target = new A2aServer(source.getName(), source.getDescription(), targetEnvironment);

        target.setEnabled(false);
        target.setAuthenticationRequired(source.isAuthenticationRequired());
        target.setUuid(source.getUuid());
        target.setWorkspaceId(source.getWorkspaceId());

        return a2aServerService.create(target);
    }

    private Optional<A2aServer> findTarget(A2aServer source, Environment targetEnvironment) {
        return a2aServerService.fetchA2aServer(Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);
    }

    private A2aServer loadSource(long sourceId, Environment targetEnvironment) {
        A2aServer source = a2aServerService.getA2aServer(sourceId);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "A2A server id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    private long workspaceId(List<SourceProject> sourceProjects) {
        if (sourceProjects.isEmpty()) {
            return 0L;
        }

        Project project = projectService.getProject(ServerProjectPromoter.getProjectId(sourceProjects.getFirst()
            .projectDeployment()));

        return project.getWorkspaceId();
    }

    private SourceProjectView sourceProjectView(SourceProject sourceProject) {
        return new SourceProjectView(
            sourceProject.projectDeployment(), sourceParameters(sourceProject), () -> workflowIds(sourceProject));
    }

    private ServerProjectStore serverProjectStore() {
        return new ServerProjectStore() {

            @Override
            public TargetProject createTargetProject(
                long targetServerId, long projectId, int projectVersion, List<String> workflowIds) {

                A2aProject createdA2aProject =
                    a2aProjectFacade.createA2aProject(targetServerId, projectId, projectVersion, workflowIds);

                return new TargetProject(
                    Objects.requireNonNull(createdA2aProject.getId(), "id"),
                    getProjectDeployment(createdA2aProject));
            }

            @Override
            public List<Mapping> mappings(long serverProjectId) {
                return A2aServerPromotionHandler.this.mappings(serverProjectId);
            }

            @Override
            public long createMapping(long serverProjectId, long projectDeploymentWorkflowId) {
                return A2aServerPromotionHandler.this.createMapping(serverProjectId, projectDeploymentWorkflowId);
            }

            @Override
            public void deleteMapping(long mappingId) {
                a2aProjectWorkflowService.delete(mappingId);
            }

            @Override
            public void updateMappingParameters(long mappingId, Map<String, ?> parameters) {
                a2aProjectWorkflowService.updateParameters(mappingId, parameters);
            }
        };
    }

    private static List<ProjectDeployment> sourceProjectDeployments(List<SourceProject> sourceProjects) {
        List<ProjectDeployment> sourceProjectDeployments = new ArrayList<>();

        for (SourceProject sourceProject : sourceProjects) {
            sourceProjectDeployments.add(sourceProject.projectDeployment());
        }

        return sourceProjectDeployments;
    }

    private Map<Long, Deque<TargetProject>> targetProjects(A2aServer target) {
        List<TargetProject> targetProjects = new ArrayList<>();

        for (A2aProject a2aProject : sortedA2aProjects(Objects.requireNonNull(target.getId(), "id"))) {
            targetProjects.add(
                new TargetProject(
                    Objects.requireNonNull(a2aProject.getId(), "id"), getProjectDeployment(a2aProject)));
        }

        return ServerProjectPromoter.groupByProjectId(targetProjects);
    }

    private List<SourceProject> sourceProjects(A2aServer source) {
        List<SourceProject> sourceProjects = new ArrayList<>();

        for (A2aProject a2aProject : sortedA2aProjects(Objects.requireNonNull(source.getId(), "id"))) {
            long a2aProjectId = Objects.requireNonNull(a2aProject.getId(), "id");

            sourceProjects.add(
                new SourceProject(
                    getProjectDeployment(a2aProject),
                    a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId)));
        }

        return sourceProjects;
    }

    private List<A2aProject> sortedA2aProjects(long a2aServerId) {
        List<A2aProject> a2aProjects = new ArrayList<>(a2aProjectService.getA2aServerA2aProjects(a2aServerId));

        a2aProjects.sort(Comparator.comparing(A2aProject::getId));

        return a2aProjects;
    }

    private List<String> workflowIds(SourceProject sourceProject) {
        Map<Long, String> workflowIdsByProjectDeploymentWorkflowId = new LinkedHashMap<>();

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(sourceProject.projectDeployment()
                    .getId(), "id"));

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            workflowIdsByProjectDeploymentWorkflowId.put(
                projectDeploymentWorkflow.getId(), projectDeploymentWorkflow.getWorkflowId());
        }

        List<String> workflowIds = new ArrayList<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : sourceProject.a2aProjectWorkflows()) {
            String workflowId =
                workflowIdsByProjectDeploymentWorkflowId.get(a2aProjectWorkflow.getProjectDeploymentWorkflowId());

            if (workflowId != null && !workflowIds.contains(workflowId)) {
                workflowIds.add(workflowId);
            }
        }

        return workflowIds;
    }

    private long createMapping(long a2aProjectId, long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow =
            a2aProjectWorkflowService.create(a2aProjectId, projectDeploymentWorkflowId);

        return Objects.requireNonNull(a2aProjectWorkflow.getId(), "id");
    }

    private List<Mapping> mappings(long a2aProjectId) {
        List<Mapping> mappings = new ArrayList<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProjectId)) {

            Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                mappings.add(
                    new Mapping(
                        Objects.requireNonNull(a2aProjectWorkflow.getId(), "id"), projectDeploymentWorkflowId));
            }
        }

        return mappings;
    }

    private ProjectDeployment getProjectDeployment(A2aProject a2aProject) {
        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(a2aProject.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private static Map<Long, Map<String, ?>> sourceParameters(SourceProject sourceProject) {
        Map<Long, Map<String, ?>> sourceParameters = new LinkedHashMap<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : sourceProject.a2aProjectWorkflows()) {
            Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                sourceParameters.put(projectDeploymentWorkflowId, a2aProjectWorkflow.getParameters());
            }
        }

        return sourceParameters;
    }
}

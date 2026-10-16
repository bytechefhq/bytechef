/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowCopyDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class ConnectedUserWorkflowReferenceAdminFacadeImpl
    implements ConnectedUserWorkflowReferenceAdminFacade {
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserService connectedUserService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceAdminFacadeImpl(
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserService = connectedUserService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public List<ConnectedUserWorkflowCopyDTO> getCopies(Set<String> automationWorkflowUuids, Environment environment) {
        if (automationWorkflowUuids.isEmpty()) {
            return List.of();
        }

        List<ConnectedUserProjectWorkflow> copies =
            connectedUserProjectWorkflowService.getCopiedConnectedUserProjectWorkflows(automationWorkflowUuids);

        Map<Long, ConnectedUser> connectedUsersByConnectedUserProjectId = getConnectedUsersByConnectedUserProjectId(
            getConnectedUserProjects(copies));

        return copies.stream()
            .filter(copy -> {
                ConnectedUser connectedUser = connectedUsersByConnectedUserProjectId.get(
                    copy.getConnectedUserProjectId());

                return connectedUser.getEnvironment() == environment;
            })
            .map(copy -> toCopyDTO(
                copy, connectedUsersByConnectedUserProjectId.get(copy.getConnectedUserProjectId()), environment))
            .toList();
    }

    @Override
    public List<ConnectedUserWorkflowReferenceDTO> getReferences(
        Set<String> automationWorkflowUuids, Environment environment) {

        List<ConnectedUserProjectWorkflow> references =
            connectedUserProjectWorkflowRepository.findAllByAutomationWorkflowUuidIn(automationWorkflowUuids);

        List<ConnectedUserProject> connectedUserProjects = getConnectedUserProjects(references);

        Map<Long, ConnectedUser> connectedUsersByConnectedUserProjectId = getConnectedUsersByConnectedUserProjectId(
            connectedUserProjects);
        Map<Long, String> workflowIds = getWorkflowIds(connectedUserProjects);

        return references.stream()
            .filter(reference -> {
                ConnectedUser connectedUser = connectedUsersByConnectedUserProjectId.get(
                    reference.getConnectedUserProjectId());

                return connectedUser.getEnvironment() == environment;
            })
            .map(reference -> toReferenceDTO(
                reference, connectedUsersByConnectedUserProjectId.get(reference.getConnectedUserProjectId()),
                workflowIds))
            .toList();
    }

    private List<ConnectedUserProject> getConnectedUserProjects(
        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows) {

        List<Long> connectedUserProjectIds = connectedUserProjectWorkflows.stream()
            .map(ConnectedUserProjectWorkflow::getConnectedUserProjectId)
            .distinct()
            .toList();

        return connectedUserProjectService.getConnectedUserProjects(connectedUserProjectIds);
    }

    private Map<Long, ConnectedUser> getConnectedUsersByConnectedUserProjectId(
        List<ConnectedUserProject> connectedUserProjects) {

        List<Long> connectedUserIds = connectedUserProjects.stream()
            .map(ConnectedUserProject::getConnectedUserId)
            .distinct()
            .toList();

        Map<Long, ConnectedUser> connectedUserMap = connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

        return connectedUserProjects.stream()
            .collect(Collectors.toMap(
                ConnectedUserProject::getId,
                connectedUserProject -> connectedUserMap.get(connectedUserProject.getConnectedUserId())));
    }

    private Map<Long, String> getWorkflowIds(List<ConnectedUserProject> connectedUserProjects) {
        List<Long> projectIds = connectedUserProjects.stream()
            .map(ConnectedUserProject::getProjectId)
            .distinct()
            .toList();

        return projectWorkflowService.getProjectWorkflows(projectIds)
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getId, ProjectWorkflow::getWorkflowId));
    }

    private boolean isCopyEnabled(ProjectWorkflow projectWorkflow, Environment environment) {
        return projectDeploymentService.fetchProjectDeployment(projectWorkflow.getProjectId(), environment)
            .flatMap(projectDeployment -> projectWorkflowService
                .fetchProjectWorkflowWorkflowId(projectDeployment.getId(), projectWorkflow.getUuidAsString())
                .map(workflowId -> projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                    projectDeployment.getId(), workflowId)))
            .orElse(false);
    }

    private ConnectedUserWorkflowCopyDTO toCopyDTO(
        ConnectedUserProjectWorkflow copy, ConnectedUser connectedUser, Environment environment) {

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            Objects.requireNonNull(copy.getProjectWorkflowId()));

        return new ConnectedUserWorkflowCopyDTO(
            copy.getCopiedFromWorkflowUuid(), projectWorkflow.getUuidAsString(), connectedUser.getExternalId(),
            connectedUser.getName(), connectedUser.getEmail(), isCopyEnabled(projectWorkflow, environment));
    }

    private ConnectedUserWorkflowReferenceDTO toReferenceDTO(
        ConnectedUserProjectWorkflow reference, ConnectedUser connectedUser, Map<Long, String> workflowIds) {

        Environment environment = connectedUser.getEnvironment();

        return new ConnectedUserWorkflowReferenceDTO(
            reference.getAutomationWorkflowUuid(), connectedUser.getExternalId(), environment.name(),
            reference.isEnabled(), reference.isDangling(), reference.getDanglingReason(),
            workflowIds.get(reference.getProjectWorkflowId()), reference.getProjectDeploymentId());
    }
}

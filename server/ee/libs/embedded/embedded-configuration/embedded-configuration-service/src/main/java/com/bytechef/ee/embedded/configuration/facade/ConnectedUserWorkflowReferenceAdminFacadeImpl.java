/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
    private final ConnectedUserService connectedUserService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceAdminFacadeImpl(
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserService connectedUserService, ProjectWorkflowService projectWorkflowService) {
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserService = connectedUserService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public List<ConnectedUserWorkflowReferenceDTO> getReferences(
        Set<String> automationWorkflowUuids, Environment environment) {

        List<ConnectedUserProjectWorkflow> references =
            connectedUserProjectWorkflowRepository.findAllByAutomationWorkflowUuidIn(automationWorkflowUuids);

        List<Long> connectedUserProjectIds = references.stream()
            .map(ConnectedUserProjectWorkflow::getConnectedUserProjectId)
            .distinct()
            .toList();

        Map<Long, ConnectedUserProject> connectedUserProjectMap =
            connectedUserProjectService.getConnectedUserProjects(connectedUserProjectIds)
                .stream()
                .collect(Collectors.toMap(ConnectedUserProject::getId, Function.identity()));

        List<Long> connectedUserIds = connectedUserProjectMap.values()
            .stream()
            .map(ConnectedUserProject::getConnectedUserId)
            .distinct()
            .toList();

        Map<Long, ConnectedUser> connectedUserMap = connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

        List<Long> projectIds = connectedUserProjectMap.values()
            .stream()
            .map(ConnectedUserProject::getProjectId)
            .distinct()
            .toList();

        Map<Long, String> workflowIds = projectWorkflowService.getProjectWorkflows(projectIds)
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getId, ProjectWorkflow::getWorkflowId));

        return references.stream()
            .filter(reference -> {
                ConnectedUserProject connectedUserProject = connectedUserProjectMap.get(
                    reference.getConnectedUserProjectId());

                ConnectedUser connectedUser = connectedUserMap.get(connectedUserProject.getConnectedUserId());

                return connectedUser.getEnvironment() == environment;
            })
            .map(reference -> toDTO(reference, connectedUserProjectMap, connectedUserMap, workflowIds))
            .toList();
    }

    private ConnectedUserWorkflowReferenceDTO toDTO(
        ConnectedUserProjectWorkflow reference, Map<Long, ConnectedUserProject> connectedUserProjectMap,
        Map<Long, ConnectedUser> connectedUserMap, Map<Long, String> workflowIds) {
        ConnectedUserProject connectedUserProject = connectedUserProjectMap.get(
            reference.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserMap.get(connectedUserProject.getConnectedUserId());

        Environment environment = connectedUser.getEnvironment();

        return new ConnectedUserWorkflowReferenceDTO(
            reference.getAutomationWorkflowUuid(), connectedUser.getExternalId(), environment.name(),
            reference.isEnabled(), reference.isDangling(), reference.getDanglingReason(),
            workflowIds.get(reference.getProjectWorkflowId()), reference.getProjectDeploymentId());
    }
}

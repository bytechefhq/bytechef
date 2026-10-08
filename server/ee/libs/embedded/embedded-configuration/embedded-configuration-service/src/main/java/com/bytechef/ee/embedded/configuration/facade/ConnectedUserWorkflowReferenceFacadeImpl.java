/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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
@SkipAutomationAuthorization
public class ConnectedUserWorkflowReferenceFacadeImpl implements ConnectedUserWorkflowReferenceFacade {
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectUserProjectRepository connectUserProjectRepository;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceFacadeImpl(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectUserProjectRepository connectUserProjectRepository,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager,
        ConnectedUserService connectedUserService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectUserProjectRepository = connectUserProjectRepository;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
        this.connectedUserService = connectedUserService;
    }

    @Override
    public void deleteReference(String externalUserId, String automationWorkflowUuid, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationWorkflowUuid, environment);

        removeRow(reference);

        connectedUserProjectWorkflowRepository.deleteById(reference.getId());
    }

    @Override
    @Transactional(noRollbackFor = {
        DanglingReferenceException.class, MissingConnectionException.class, MissingInputException.class
    })
    public void enableReference(
        String externalUserId, String automationWorkflowUuid, boolean enable, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationWorkflowUuid, environment);

        if (!enable) {
            if (reference.isDangling()) {
                removeRow(reference);
            } else {
                applyWorkflow(reference, externalUserId, environment, false, true, Map.of(), null);
            }

            return;
        }

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationWorkflowUuid);
        }

        reference = catchUp(reference);

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationWorkflowUuid);
        }

        applyWorkflow(reference, externalUserId, environment, true, true, Map.of(), null);
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        return connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment) {
        return provisionReference(externalUserId, automationWorkflowUuid, environment, Map.of(), null);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds) {
        return provisionReference(externalUserId, automationWorkflowUuid, environment, requestedConnectionIds, null);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {
        return provisionReference(externalUserId, automationWorkflowUuid, environment, requestedConnectionIds, inputs);
    }

    private ConnectedUserProjectWorkflow provisionReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {
        ConnectedUserProject connectedUserProject = lockConnectedUserProject(externalUserId, environment);

        Optional<ConnectedUserProjectWorkflow> existingReference = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationWorkflowUuid(connectedUserProject.getId(),
                automationWorkflowUuid);

        if (existingReference.isPresent()) {
            ConnectedUserProjectWorkflow reference = catchUp(existingReference.get());

            if ((!requestedConnectionIds.isEmpty() || inputs != null) && !reference.isDangling()) {
                return applyWorkflow(
                    reference, externalUserId, environment, reference.isEnabled(), false, requestedConnectionIds,
                    inputs);
            }

            return reference;
        }

        AutomationWorkflowProjectDTO automationWorkflowProject = getVisibleAutomationWorkflowProject(
            externalUserId, automationWorkflowUuid, environment);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProject.id(), externalUserId, environment);

        if (connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)) {
            projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
                automationWorkflowProject.id(), externalUserId, environment);
        }

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setAutomationWorkflowUuid(automationWorkflowUuid);
        reference.setConnectedUserProjectId(connectedUserProject.getId());
        reference.setEnabled(false);
        reference.setProjectDeploymentId(projectDeploymentId);

        ConnectedUserProjectWorkflow savedReference = connectedUserProjectWorkflowRepository.save(reference);

        return applyWorkflow(
            savedReference, externalUserId, environment, true, false, requestedConnectionIds, inputs);
    }

    @Override
    public void updateReferenceInputs(
        String externalUserId, String automationWorkflowUuid, Map<String, ?> inputs, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationWorkflowUuid, environment);

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationWorkflowUuid);
        }

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), automationWorkflowUuid, inputs);
    }

    private ConnectedUserProjectWorkflow applyWorkflow(
        ConnectedUserProjectWorkflow reference, String externalUserId, Environment environment, boolean enable,
        boolean throwOnMissingInput, Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);
        ProjectDeployment projectDeployment = connectedUserReferenceDeploymentManager.getDeployment(
            reference.getProjectDeploymentId());

        int projectVersion = projectDeployment.getProjectVersion();
        String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

        String workflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            projectDeployment.getProjectId(), projectVersion, automationWorkflowUuid);

        Optional<ProjectDeploymentWorkflow> currentRow = connectedUserReferenceDeploymentManager.fetchWorkflowRow(
            reference.getProjectDeploymentId(), workflowId);

        List<ProjectDeploymentWorkflowConnection> currentConnections = currentRow
            .map(ProjectDeploymentWorkflow::getConnections)
            .orElse(List.of());
        Map<String, ?> currentInputs = inputs == null
            ? currentRow.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
                .orElse(Map.of())
            : inputs;

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUser.getId(), workflowId, enable, requestedConnectionIds, currentConnections, currentInputs);

        RowSpec resolvedRowSpec = resolution.rowSpec();

        RowSpec rowSpec = inputs == null
            ? resolvedRowSpec : new RowSpec(resolvedRowSpec.resolved(), resolvedRowSpec.enabled(), inputs);

        connectedUserReferenceDeploymentManager.putWorkflows(
            reference.getProjectDeploymentId(), projectVersion, Map.of(automationWorkflowUuid, rowSpec));

        reference.setEnabled(rowSpec.enabled());

        ConnectedUserProjectWorkflow savedReference = connectedUserProjectWorkflowRepository.save(reference);

        if (enable && resolution.missingComponentName() != null) {
            throw new MissingConnectionException(resolution.missingComponentName());
        }

        if (enable && throwOnMissingInput && resolution.missingInputName() != null) {
            throw new MissingInputException(resolution.missingInputName());
        }

        return savedReference;
    }

    private ConnectedUserProjectWorkflow catchUp(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId == null || isDanglingWithoutDeployment(reference, projectDeploymentId) ||
            !connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)) {
            return reference;
        }

        return connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow();
    }

    private AutomationWorkflowProjectDTO getVisibleAutomationWorkflowProject(
        String externalUserId, String automationWorkflowUuid, Environment environment) {
        return automationWorkflowProjectFacade.getPublishedProjects(externalUserId, environment)
            .stream()
            .filter(project -> CollectionUtils.stream(project.workflowTemplates())
                .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), automationWorkflowUuid)))
            .findFirst()
            .orElseThrow(() -> new AutomationWorkflowTemplateNotVisibleException(automationWorkflowUuid));
    }

    private boolean isDanglingWithoutDeployment(ConnectedUserProjectWorkflow reference, long projectDeploymentId) {
        if (!reference.isDangling()) {
            return false;
        }

        Optional<ProjectDeployment> projectDeployment = connectedUserReferenceDeploymentManager.fetchDeployment(
            projectDeploymentId);

        return projectDeployment.isEmpty();
    }

    private ConnectedUserProject lockConnectedUserProject(String externalUserId, Environment environment) {
        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        return connectUserProjectRepository.findByIdForUpdate(connectedUserProject.getId())
            .orElseThrow();
    }

    private void removeRow(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId != null) {
            connectedUserReferenceDeploymentManager.removeWorkflow(
                projectDeploymentId, reference.getAutomationWorkflowUuid());
        }
    }

    private ConnectedUserProjectWorkflow requireReference(
        String externalUserId, String automationWorkflowUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = lockConnectedUserProject(externalUserId, environment);

        return connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationWorkflowUuid(connectedUserProject.getId(), automationWorkflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "No reference to automation workflow: %s".formatted(automationWorkflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));
    }
}

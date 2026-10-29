/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
public class ConnectedUserProjectAdminFacadeImpl implements ConnectedUserProjectAdminFacade {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectAdminFacadeImpl(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConnectedUserService connectedUserService,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
    }

    @Override
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        connectedUserProjectFacade.deleteProjectWorkflow(connectedUserProjectWorkflowId);
    }

    @Override
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        connectedUserProjectFacade.enableProjectWorkflow(connectedUserProjectWorkflowId, enable);
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, environment);
    }

    @Override
    public List<ProvisionableAutomationWorkflowProjectDTO> getProvisionableProjects(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid = connectedUserWorkflowReferenceFacade
            .getConnectedUserWorkflows(connectedUserId)
            .stream()
            .collect(
                Collectors.toMap(
                    ConnectedUserProjectWorkflow::getAutomationWorkflowUuid, Function.identity(),
                    (firstReference, secondReference) -> firstReference));

        return automationWorkflowProjectFacade
            .getPublishedProjects(connectedUser.getExternalId(), connectedUser.getEnvironment())
            .stream()
            .map(project -> toProvisionableProject(project, referencesByWorkflowUuid))
            .sorted(
                Comparator.comparing(ProvisionableAutomationWorkflowProjectDTO::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private static ProvisionableAutomationWorkflowProjectDTO toProvisionableProject(
        AutomationWorkflowProjectDTO project, Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        List<ProvisionableAutomationWorkflowProjectDTO.Workflow> workflows = project.workflowTemplates()
            .stream()
            .map(template -> toProvisionableWorkflow(template, referencesByWorkflowUuid.get(template.workflowUuid())))
            .toList();

        return new ProvisionableAutomationWorkflowProjectDTO(
            project.id(), project.name(), project.lastPublishedVersion(), workflows);
    }

    private static ProvisionableAutomationWorkflowProjectDTO.Workflow toProvisionableWorkflow(
        ConnectedUserWorkflowTemplateDTO template, ConnectedUserProjectWorkflow reference) {

        return new ProvisionableAutomationWorkflowProjectDTO.Workflow(
            template.workflowUuid(), template.label(), template.inputs(), reference != null,
            reference != null && reference.isDangling());
    }
}

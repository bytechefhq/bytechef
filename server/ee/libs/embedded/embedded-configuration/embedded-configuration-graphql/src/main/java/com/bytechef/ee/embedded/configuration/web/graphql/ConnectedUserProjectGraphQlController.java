/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectAdminFacade;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserProjectGraphQlController {

    private final ConnectedUserProjectAdminFacade connectedUserProjectFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectGraphQlController(
        ConnectedUserProjectAdminFacade connectedUserProjectFacade, EnvironmentService environmentService) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<ConnectedUserProjectDTO> connectedUserProjects(
        @Argument Long connectedUserId, @Argument Long environmentId) {

        return connectedUserProjectFacade.getConnectedUserProjects(
            connectedUserId, environmentService.getEnvironment(environmentId));
    }

    @QueryMapping
    public List<ProvisionableAutomationProjectDTO> provisionableConnectedUserProjects(
        @Argument long connectedUserId) {

        return connectedUserProjectFacade.getProvisionableProjects(connectedUserId);
    }

    @MutationMapping
    public boolean deleteConnectedUserProjectWorkflow(@Argument long id) {
        connectedUserProjectFacade.deleteProjectWorkflow(id);

        return true;
    }

    @MutationMapping
    public boolean enableConnectedUserProjectWorkflow(@Argument long id, @Argument boolean enable) {
        connectedUserProjectFacade.enableProjectWorkflow(id, enable);

        return true;
    }

    @SchemaMapping(typeName = "ConnectedUserProject")
    public List<ConnectedUserAutomationProjectConfigurationDTO> automationProjectConfigurations(
        ConnectedUserProjectDTO connectedUserProject) {

        ConnectedUser connectedUser = connectedUserProject.connectedUser();

        return connectedUserProjectFacade.getConnectedUserProjectConfigurations(
            Objects.requireNonNull(connectedUser.getId()));
    }

    @MutationMapping
    public boolean provisionConnectedUserProjectWorkflows(
        @Argument long connectedUserId, @Argument long configurationId,
        @Argument List<ProvisionConnectedUserProjectWorkflowInput> workflows) {

        connectedUserProjectFacade.provisionProjectWorkflows(
            connectedUserId, configurationId,
            workflows.stream()
                .map(workflow -> new ProvisionConnectedUserProjectWorkflowDTO(
                    workflow.workflowUuid(), workflow.inputs(), getConnectionIds(workflow.connections())))
                .toList());

        return true;
    }

    private static Map<String, Long> getConnectionIds(
        @Nullable List<ProvisionConnectedUserProjectWorkflowConnectionInput> connections) {

        if (connections == null) {
            return Map.of();
        }

        Map<String, Long> connectionIds = new LinkedHashMap<>();

        for (ProvisionConnectedUserProjectWorkflowConnectionInput connection : connections) {
            if (connectionIds.putIfAbsent(connection.componentName(), connection.connectionId()) != null) {
                throw new IllegalArgumentException(
                    "Component %s has more than one connection".formatted(connection.componentName()));
            }
        }

        return connectionIds;
    }

    @SuppressFBWarnings("EI")
    public record ProvisionConnectedUserProjectWorkflowInput(
        String workflowUuid, Map<String, Object> inputs,
        @Nullable List<ProvisionConnectedUserProjectWorkflowConnectionInput> connections) {
    }

    public record ProvisionConnectedUserProjectWorkflowConnectionInput(String componentName, long connectionId) {
    }
}

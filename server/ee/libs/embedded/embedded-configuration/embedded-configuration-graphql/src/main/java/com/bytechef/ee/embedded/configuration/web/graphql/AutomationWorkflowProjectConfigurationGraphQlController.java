/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectConfigurationAdminFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationWorkflowProjectConfigurationGraphQlController {

    private final AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationGraphQlController(
        AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade) {

        this.automationWorkflowProjectConfigurationAdminFacade = automationWorkflowProjectConfigurationAdminFacade;
    }

    @QueryMapping
    public List<AutomationWorkflowProjectConfigurationDTO> automationWorkflowProjectConfigurations(
        @Argument String projectId, @Argument @Nullable String environmentId) {

        if (environmentId == null) {
            return automationWorkflowProjectConfigurationAdminFacade.getConfigurations(Long.parseLong(projectId));
        }

        return automationWorkflowProjectConfigurationAdminFacade.getConfigurations(
            Long.parseLong(projectId), Long.parseLong(environmentId));
    }

    @QueryMapping
    public AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO
        automationWorkflowProjectConfigurationWorkflowProvidableValues(
            @Argument String id, @Argument String workflowUuid) {

        return automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(
            Long.parseLong(id), workflowUuid);
    }

    @QueryMapping
    public List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        automationWorkflowProjectConfigurationWorkflowInputOptions(
            @Argument String id, @Argument String workflowUuid, @Argument String inputName,
            @Argument String propertyName, @Argument String connectionId,
            @Argument @Nullable Map<String, Object> lookupDependsOnValues, @Argument @Nullable String searchText) {

        return automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowInputOptions(
            Long.parseLong(id), workflowUuid, inputName, propertyName, parseConnectionId(inputName, connectionId),
            lookupDependsOnValues == null ? Map.of() : lookupDependsOnValues, searchText);
    }

    @QueryMapping
    public AutomationWorkflowProjectConfigurationVersionImpactDTO automationWorkflowProjectConfigurationVersionImpact(
        @Argument String id, @Argument int projectVersion) {

        return automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(Long.parseLong(id), projectVersion);
    }

    @QueryMapping
    public List<AutomationWorkflowProjectConfigurationConnectedUserDTO>
        automationWorkflowProjectConfigurationConnectedUsers(@Argument String id) {

        return automationWorkflowProjectConfigurationAdminFacade.getConfigurationConnectedUsers(Long.parseLong(id));
    }

    @QueryMapping
    public AutomationWorkflowProjectConfigurationDeleteImpactDTO automationWorkflowProjectConfigurationDeleteImpact(
        @Argument String id) {

        return automationWorkflowProjectConfigurationAdminFacade.getDeleteImpact(Long.parseLong(id));
    }

    @QueryMapping
    public ConfigurationMoveImpactDTO automationWorkflowProjectConfigurationMoveImpact(
        @Argument String sourceId, @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationWorkflowProjectConfigurationAdminFacade.getMoveImpact(
            Long.parseLong(sourceId), Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @MutationMapping
    public String createAutomationWorkflowProjectConfiguration(
        @Argument String projectId, @Argument String environmentId, @Argument String name,
        @Argument @Nullable String permissionExpression, @Argument int projectVersion, @Argument boolean enabled,
        @Argument boolean automationHubVisible) {

        return String.valueOf(
            automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                Long.parseLong(projectId), Long.parseLong(environmentId), name, permissionExpression, projectVersion,
                enabled, automationHubVisible));
    }

    @MutationMapping
    public boolean deleteAutomationWorkflowProjectConfiguration(@Argument String id) {
        automationWorkflowProjectConfigurationAdminFacade.deleteConfiguration(Long.parseLong(id));

        return true;
    }

    @MutationMapping
    public boolean enableAutomationWorkflowProjectConfiguration(@Argument String id, @Argument boolean enable) {
        automationWorkflowProjectConfigurationAdminFacade.enableConfiguration(Long.parseLong(id), enable);

        return true;
    }

    @MutationMapping
    public boolean enableAutomationWorkflowProjectConfigurationWorkflow(
        @Argument String id, @Argument String workflowUuid, @Argument boolean enable) {

        automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            Long.parseLong(id), workflowUuid, enable);

        return true;
    }

    @MutationMapping
    public ConfigurationMoveResultDTO moveAutomationWorkflowProjectConfigurationConnectedUsers(
        @Argument String sourceId, @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationWorkflowProjectConfigurationAdminFacade.moveConnectedUsers(
            Long.parseLong(sourceId), Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectConfiguration(
        @Argument String id, @Argument String name, @Argument @Nullable String permissionExpression,
        @Argument int projectVersion, @Argument boolean enabled, @Argument boolean automationHubVisible) {

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
            Long.parseLong(id), name, permissionExpression, projectVersion, enabled, automationHubVisible);

        return true;
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectConfigurationWorkflow(
        @Argument String id, @Argument String workflowUuid, @Argument Map<String, Object> providedConnections,
        @Argument Map<String, Object> providedInputs) {

        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            Long.parseLong(id), workflowUuid, toConnectionIds(providedConnections),
            providedInputs == null ? Map.of() : providedInputs);

        return true;
    }

    private static List<Long> toLongs(List<String> ids) {
        return ids.stream()
            .map(Long::parseLong)
            .toList();
    }

    private static Map<String, Long> toConnectionIds(Map<String, Object> providedConnections) {
        Map<String, Long> connectionIds = new HashMap<>();

        if (providedConnections == null) {
            return connectionIds;
        }

        for (Map.Entry<String, Object> providedConnection : providedConnections.entrySet()) {
            String componentName = providedConnection.getKey();
            Object connectionId = providedConnection.getValue();

            if (connectionId == null) {
                connectionIds.put(componentName, null);
            } else if (connectionId instanceof Number number) {
                connectionIds.put(componentName, number.longValue());
            } else {
                connectionIds.put(componentName, parseConnectionId(componentName, String.valueOf(connectionId)));
            }
        }

        return connectionIds;
    }

    private static long parseConnectionId(String componentName, String connectionId) {
        try {
            return Long.parseLong(connectionId);
        } catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                "Connection id '%s' for component %s is not a number".formatted(connectionId, componentName),
                numberFormatException, AutomationWorkflowProjectConfigurationErrorType.INVALID_PROVIDED_VALUE);
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectConfigurationAdminFacade;
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
public class AutomationProjectConfigurationGraphQlController {

    private final AutomationProjectConfigurationAdminFacade automationProjectConfigurationAdminFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectConfigurationGraphQlController(
        AutomationProjectConfigurationAdminFacade automationProjectConfigurationAdminFacade) {

        this.automationProjectConfigurationAdminFacade = automationProjectConfigurationAdminFacade;
    }

    @QueryMapping
    public List<AutomationProjectConfigurationDTO> automationProjectConfigurations(
        @Argument String projectId, @Argument @Nullable String environmentId) {

        if (environmentId == null) {
            return automationProjectConfigurationAdminFacade.getConfigurations(Long.parseLong(projectId));
        }

        return automationProjectConfigurationAdminFacade.getConfigurations(
            Long.parseLong(projectId), Long.parseLong(environmentId));
    }

    @QueryMapping
    public AutomationProjectConfigurationWorkflowProvidableValuesDTO
        automationProjectConfigurationWorkflowProvidableValues(
            @Argument String id, @Argument String workflowUuid) {

        return automationProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(
            Long.parseLong(id), workflowUuid);
    }

    @QueryMapping
    public List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        automationProjectConfigurationWorkflowInputOptions(
            @Argument String id, @Argument String workflowUuid, @Argument String inputName,
            @Argument String propertyName, @Argument String connectionId,
            @Argument @Nullable Map<String, Object> lookupDependsOnValues, @Argument @Nullable String searchText) {

        return automationProjectConfigurationAdminFacade.getConfigurationWorkflowInputOptions(
            Long.parseLong(id), workflowUuid, inputName, propertyName, parseConnectionId(inputName, connectionId),
            lookupDependsOnValues == null ? Map.of() : lookupDependsOnValues, searchText);
    }

    @QueryMapping
    public AutomationProjectConfigurationVersionImpactDTO automationProjectConfigurationVersionImpact(
        @Argument String id, @Argument int projectVersion) {

        return automationProjectConfigurationAdminFacade.getVersionImpact(Long.parseLong(id), projectVersion);
    }

    @QueryMapping
    public List<AutomationProjectConfigurationConnectedUserDTO>
        automationProjectConfigurationConnectedUsers(@Argument String id) {

        return automationProjectConfigurationAdminFacade.getConfigurationConnectedUsers(Long.parseLong(id));
    }

    @QueryMapping
    public AutomationProjectConfigurationDeleteImpactDTO automationProjectConfigurationDeleteImpact(
        @Argument String id) {

        return automationProjectConfigurationAdminFacade.getDeleteImpact(Long.parseLong(id));
    }

    @QueryMapping
    public ConfigurationMoveImpactDTO automationProjectConfigurationUpgradeImpact(
        @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationProjectConfigurationAdminFacade.getUpgradeImpact(
            Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @QueryMapping
    public ConfigurationMoveImpactDTO automationProjectConfigurationMoveImpact(
        @Argument String sourceId, @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationProjectConfigurationAdminFacade.getMoveImpact(
            Long.parseLong(sourceId), Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @MutationMapping
    public String createAutomationProjectConfiguration(
        @Argument String projectId, @Argument String environmentId, @Argument String name,
        @Argument @Nullable String permissionExpression, @Argument int projectVersion, @Argument boolean enabled,
        @Argument boolean automationHubVisible) {

        return String.valueOf(
            automationProjectConfigurationAdminFacade.createConfiguration(
                Long.parseLong(projectId), Long.parseLong(environmentId), name, permissionExpression, projectVersion,
                enabled, automationHubVisible));
    }

    @MutationMapping
    public boolean deleteAutomationProjectConfiguration(@Argument String id) {
        automationProjectConfigurationAdminFacade.deleteConfiguration(Long.parseLong(id));

        return true;
    }

    @MutationMapping
    public boolean enableAutomationProjectConfiguration(@Argument String id, @Argument boolean enable) {
        automationProjectConfigurationAdminFacade.enableConfiguration(Long.parseLong(id), enable);

        return true;
    }

    @MutationMapping
    public boolean enableAutomationProjectConfigurationWorkflow(
        @Argument String id, @Argument String workflowUuid, @Argument boolean enable) {

        automationProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            Long.parseLong(id), workflowUuid, enable);

        return true;
    }

    @MutationMapping
    public ConfigurationMoveResultDTO moveAutomationProjectConfigurationConnectedUsers(
        @Argument String sourceId, @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationProjectConfigurationAdminFacade.moveConnectedUsers(
            Long.parseLong(sourceId), Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @MutationMapping
    public ConfigurationMoveResultDTO upgradeAutomationProjectConfigurationConnectedUsers(
        @Argument String targetId, @Argument List<String> connectedUserIds) {

        return automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
            Long.parseLong(targetId), toLongs(connectedUserIds));
    }

    @MutationMapping
    public boolean updateAutomationProjectConfiguration(
        @Argument String id, @Argument String name, @Argument @Nullable String permissionExpression,
        @Argument int projectVersion, @Argument boolean enabled, @Argument boolean automationHubVisible) {

        automationProjectConfigurationAdminFacade.updateConfiguration(
            Long.parseLong(id), name, permissionExpression, projectVersion, enabled, automationHubVisible);

        return true;
    }

    @MutationMapping
    public boolean updateAutomationProjectConfigurationWorkflow(
        @Argument String id, @Argument String workflowUuid, @Argument Map<String, Object> providedConnections,
        @Argument Map<String, Object> providedInputs) {

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
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
                numberFormatException, AutomationProjectConfigurationErrorType.INVALID_PROVIDED_VALUE);
        }
    }
}

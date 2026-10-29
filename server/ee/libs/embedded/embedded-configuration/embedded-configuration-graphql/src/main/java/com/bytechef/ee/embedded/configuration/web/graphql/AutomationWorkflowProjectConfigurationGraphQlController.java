/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectConfigurationAdminFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        @Argument String projectId) {

        return automationWorkflowProjectConfigurationAdminFacade.getConfigurations(Long.parseLong(projectId));
    }

    @QueryMapping
    public AutomationWorkflowProjectConfigurationVersionImpactDTO automationWorkflowProjectConfigurationVersionImpact(
        @Argument String id, @Argument int projectVersion) {

        return automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(Long.parseLong(id), projectVersion);
    }

    @MutationMapping
    public String createAutomationWorkflowProjectConfiguration(
        @Argument String projectId, @Argument String environmentId, @Argument int projectVersion,
        @Argument boolean enabled, @Argument boolean automationHubVisible) {

        return String.valueOf(
            automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                Long.parseLong(projectId), Long.parseLong(environmentId), projectVersion, enabled,
                automationHubVisible));
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
    public boolean updateAutomationWorkflowProjectConfiguration(
        @Argument String id, @Argument int projectVersion, @Argument boolean enabled,
        @Argument boolean automationHubVisible) {

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
            Long.parseLong(id), projectVersion, enabled, automationHubVisible);

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

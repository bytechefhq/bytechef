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
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectConfigurationAdminFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
}

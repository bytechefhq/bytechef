/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AutomationWorkflowProjectConfigurationTestSupport {

    private AutomationWorkflowProjectConfigurationTestSupport() {
    }

    /**
     * Publishes the project and pins an enabled production configuration to the version just published, so that the
     * production environment keeps seeing the latest published version.
     */
    public static void publishAndPinProject(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService, long projectId) {

        automationWorkflowProjectFacade.publishProject(projectId);

        int publishedVersion = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .map(AutomationWorkflowProjectDTO::lastPublishedVersion)
            .findFirst()
            .orElseThrow();

        Optional<AutomationWorkflowProjectConfiguration> configurationOptional =
            automationWorkflowProjectConfigurationService.fetchConfiguration(
                projectId, Environment.PRODUCTION.ordinal());

        if (configurationOptional.isPresent()) {
            automationWorkflowProjectConfigurationService.update(
                configurationOptional.get()
                    .getId(),
                publishedVersion, true, true);
        } else {
            automationWorkflowProjectConfigurationService.create(
                projectId, Environment.PRODUCTION.ordinal(), publishedVersion, true, true);
        }
    }
}
